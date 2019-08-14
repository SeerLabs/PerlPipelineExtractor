use warnings;
use XML::Bare;
use File::Path;
use File::Basename;
use File::Copy;
use FindBin;
use lib "$FindBin::Bin";
use FileConverter::Controller;
use DocFilter::Filter;
use ParsCit::Controller;
use HeaderParse::API::Parser;
use HeaderParse::Config::API_Config;
use Time::localtime;

my $PASS = 1;
my $FAIL = -1;

my $xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";


my $year = localtime->year() + 1900;
my $month = localtime->mon();
my $day = localtime->mday();
my $dateStr = sprintf("%04d%02d%02d",$year,$month,$day);

# Command-line arguments
my $dataset_id = $ARGV[0];
my $xml_file_path = $ARGV[1];
my $base_document_path = $ARGV[2];
my $java_results_path = $ARGV[3];
my $parse_results_path = $ARGV[4] . "/$dataset_id$dateStr";
my $batch_num = $ARGV[5];
my $out_level = $ARGV[6];

mkpath($parse_results_path) unless(-e $parse_results_path);

my $xml = XML::Bare->new( file => $xml_file_path );
my $tree = $xml->parse();
my @fileList = ();

# Read xml file of document info
foreach my $file_ptr (@{$tree->{'documents'}->{'doc'}}) {
   my $id = $file_ptr->{'id'}->{'value'};
   my $fpfragment = $file_ptr->{'value'};
   my $file = "$base_document_path/$fpfragment";
   push @fileList, {path => $file, id => $id};
}

my @failedFiles = ();
my @passedFiles = ();

my $count = 1;
my $arrSize = scalar(@fileList);

# Process each file
foreach my $file (@fileList) {
   # TODO time this out if it fails
   my $newState = process_file($file);

   if ($newState == $FAIL) {
      $out_level >= 1 && print "Batch $batch_num: FAIL $file->{id} $count/$arrSize\n";
      push @failedFiles, $file->{id};
   } elsif ($newState == $PASS) {
      $out_level >= 1 && print "Batch $batch_num: PASS $file->{id} $count/$arrSize\n";
      push @passedFiles, $file->{id};
   }
   $count += 1;
}

# Write results to file for java process to read and update DB
open(my $results, '>', $java_results_path) or die "Error opening java results path: $!";
print $results "failed:";
print $results join(',',@failedFiles) . "\n";
print $results "passed:";
print $results join(',',@passedFiles) . "\n";

sub process_file {
   # TODO implement parsing of file
   # return new state of file after processing
   my $filePath = $_[0]->{'path'};
   my $fileId = $_[0]->{'id'};
   my ($status, $msg) = prep($filePath, $fileId);
   if($status == $FAIL) {
      $out_level >= 2 && print("$msg\n");
   }
   return $status;
}


sub getFileName {
	my $filepath = $_[0];
	my @pathE = fileparse($filepath);
	my $filename = $pathE[0];
	$filename =~s/\.(pdf|ps)(\.gz)?$//g;
	return $filename;
}

sub prep {
    my ($filePath, $id) = @_;
    my $metPath = $filePath.".met";

    $filePath =~ m/^.*(\.(ps|pdf)(\.g?z)?)$/i;
    my $ext = $1;
    my $fileid = getFileName($filePath);
    my $targetPath = "$parse_results_path/$fileid$ext";
    my $targetMET = "$parse_results_path/$fileid.met";
    unless(copy($filePath, $targetPath)) {
        $out_level >= 2 && print "COPY ---------- [FAIL]\n";
	return ($FAIL, "unable to copy: $!");
    }
    $out_level >= 2 && print "COPY ---------- [OK]\n";
    
    unless(copy($metPath, $targetMET)) {
        $out_level >= 2 && print "COPY MET ------ [FAIL]\n";
	return ($FAIL, "unable to copy met file: $!");
    }
    $out_level >= 2 && print "COPY MET ------ [OK]\n";

    my $textFile;
    my $conversionSuccess = 0;

    my ($evalRet0, $evalRet1) = eval { # External call going too slow ?
	alarm(600);
         $out_level >= 2 && print "extract from: $targetPath    $fileid\n";
    	my ($status, $msg, $textPath) = extractText($targetPath, $fileid);
        $out_level >= 2 && print "extractText status: $status\n";
    	if ($status > 0) {
		$textFile = $textPath;
                # print file size
                my $filesize = -s $textFile;
                my ($lines,$words,$chars) = count_char($textFile);
                $out_level >= 2 && print "filesize: $filesize\n";
                $out_level >= 2 && print "lines: $lines\n";
                $out_level >= 2 && print "words: $words\n";
                $out_level >= 2 && print "chars: $chars\n";
		my ($status, $msg) = filter($textFile);
                $out_level >= 2 && print "filter status: $status\n";
		if ($status > 0) {
	    	    $conversionSuccess = 1;
		}
	    } else {
                $out_level >= 2 && print "CONVERS ------- [FAIL]\n";
		unlink($targetPath); unlink($targetMET);
		return ($FAIL, $msg);
    	}
	alarm(0);
    };
    if($evalRet0 == $FAIL) {
       return($FAIL, $evalRet1);
    }
    if($@=~/ERROR::/) { 
        $out_level >= 2 && print "CONVERT ------- [FAIL]\n";
	return ($FAIL,"Conversion Timeout");
    }
    $out_level >= 2 && print "CONVERT ------- [OK]\n";

    my ($status, $msg) = filter($textFile);
    $out_level >= 2 && print "filter status: $status\n";
    if ($status <= 0) {
	unlink($targetPath); 
	unlink($targetMET);
        $out_level >= 2 && print "FILTER -------- [FAIL]\n";
        $out_level >= 2 && print "$textFile\n";
	return ($FAIL, $msg);
    }
    $out_level >= 2 && print "FILTER -------- [OK]\n";
    
    ($status, $msg) = extractCitations($textFile, $fileid);
    if ($status <= 0) {
	unlink($targetPath); 
	unlink($targetMET);
	#unlink($textFile);
        $out_level >= 2 && print "CITATIONS ----- [FAIL]\n";
	return ($FAIL, $msg);
    }
    $out_level >= 2 && print "CITATIONS ----- [OK]\n";

    ($status, $msg) = extractHeader($textFile, $fileid);
    if ($status <= 0) {
	unlink($targetPath); 
	unlink($targetMET);
	#unlink($textFile);
        $out_level >= 2 && print "HEADER -------- [FAIL]\n";
	return ($FAIL, $msg);
    }
    $out_level >= 2 && print "HEADER -------- [OK]\n";
    $out_level >= 2 && print "\n";
    return ($PASS, "");
}

sub count_char() {
    my $textFile = shift;
    open(FILE, "<",$textFile) or die "Could not open file: $!";

    my ($lines, $words, $chars) = (0,0,0);

    while (<FILE>) {
        $lines++;
        $chars += length($_);
        my @matchArr = split(/\s+/, $_);
        my $numWords = @matchArr;
        $words += $numWords;
    }
    close FILE;
    return ($lines,$words,$chars);
}

sub extractText {
    my ($filePath, $id) = @_;
    my ($status, $msg, $textFile, $rTrace, $rCheckSums) =
	FileConverter::Controller::extractText($filePath);
    if ($status <= 0) {
	return ($status, $msg);
    } else {
	unless(open(FINFO, ">$parse_results_path/$id.file")) {
	    return (0, "unable to write finfo: $!");
	}
	$out_level >= 2 && print FINFO $xmlHeader;
	$out_level >= 2 && print FINFO "<conversionTrace>";
	$out_level >= 2 && print FINFO join ",", @$rTrace;
	$out_level >= 2 && print FINFO "</conversionTrace>\n";
	$out_level >= 2 && print FINFO "<checksums>\n";
	foreach my $checkSum(@$rCheckSums) {
	    $out_level >= 2 && print FINFO "<checksum>\n";
	    $out_level >= 2 && print FINFO "<fileType>".$checkSum->getFileType()."</fileType>\n";
	    $out_level >= 2 && print FINFO "<sha1>".$checkSum->getSHA1()."</sha1>\n";
	    $out_level >= 2 && print FINFO "</checksum>\n";
	}
	$out_level >= 2 && print FINFO "</checkSums>\n";
	close FINFO;
    }
    return (1, "", $textFile);
}

sub filter {
    my $textFile = shift;
    my ($sysStatus, $filterStatus, $msg) =
	DocFilter::Filter::filter($textFile);
    if ($sysStatus > 0) {
	if ($filterStatus > 0) {
	    return (1);
	} else {
	    return (0, "document failed filtration");
	}
    } else {
	return (0, "An error occurred during filtration: $msg");
    }
}

sub extractCitations {
    my ($textFile, $id) = @_;

    my $rXML = ParsCit::Controller::extractCitations($textFile);

    unless(open(CITE, ">:utf8", "$parse_results_path/$id.parscit")) {
	return (0, "Unable to open parscit file: $!");
    }

    $out_level >= 2 && print CITE $$rXML;
    close CITE;
    return (1);
}

sub extractHeader {
    my ($textFile, $id) = @_;

    my $jobID;
    while($jobID = rand(time)) {
	unless(-f $offlineD."$jobID") {
	    last;
	}
    }

    my ($status, $msg, $rXML) =
	HeaderParse::API::Parser::_parseHeader($textFile, $jobID);

    if ($status <= 0) {
	return ($status, $msg);
    }

    unless(open(HEAD, ">:utf8", "$parse_results_path/$id.header")) {
	return (0, "Unable to open header file: $!");
    }

    $out_level >= 2 && print HEAD $$rXML;
    close HEAD;
    return (1);

}
