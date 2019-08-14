# PowerShell script to run TET XSLT samples with the .NET XSLT engine
#
# usage: powershell runxsl.vbs [ <input file> <stylesheet> <output file> [ { <param name> <param value> } ... ] ]
#
# If the script is invoked without arguments, it will run all the samples.
# If the script is invoked with the arguments <input file>, <stylesheet> and
# <output file>, the script will run the given stylesheet for the input file
# and write the results to the output file. Stylesheet parameters can be
# provided as pairwise arguments at the end of the command line.
#
# $Id: runxslt.ps1,v 1.5 2008/11/12 13:22:23 tm Exp $

# Array of arrays to describe the default execution without stylesheet parameters 
$script:runs =
    ("FontReporter.tetml", "concordance.xsl", "concordance.txt"),
    ("FontReporter.tetml", "index.xsl", "index.txt"),
    ("FontReporter.tetml", "table.xsl", "table.csv"),
    ("FontReporter.tetml", "textonly.xsl", "textonly.txt"),
    ("FontReporter.tetml", "metadata.xsl", "metadata.txt"),
    ("FontReporter.tetml", "fontfilter.xsl", "fontfilter.txt"),
    ("FontReporter.tetml", "fontstat.xsl", "fontstat.txt"),
    ("FontReporter.tetml", "fontfinder.xsl", "fontfinder.txt"),
    ("FontReporter.tetml", "tetml2html.xsl", "tetml2html.html")

$script:xslt = new-object System.Xml.Xsl.XslTransform

function runXslt($inputXml, $xsl, $outputFile, $params)
{
    write-output ("Transforming input file `"" + $inputXml +  "`" with stylesheet `"" +
                $xsl + "`" to output file `"" + $outputFile + "`"")
                
    $inputXml = resolve-path $inputXml
    $xsl = resolve-path $xsl
    $outputFile = [System.IO.Path]::GetFullPath([System.IO.Path]::Combine($pwd.ToString(), $outputFile))
              
    # prepare stylesheet parameters
    $private:argList = new-object System.Xml.Xsl.XsltArgumentList
    
    $private:i
    for ($i = 0; $i -lt $params.length; $i += 2)
    {
        $argList.AddParam($params[$i], "", $params[$i + 1])
    }
    
    $private:inputXmlDocument = new-object System.Xml.XmlDocument
    $inputXmlDocument.Load($inputXml)
    $xslt.Load($xsl)
    $xslt.Transform($inputXmlDocument, $argList, (new-object System.IO.StreamWriter($outputFile)))
}

function usage
{
    write-error "usage: runxsl.ps1 [ <input file> <stylesheet> <output file> [ { <param name> <param value> } ... ] ]"
    exit 1
}

if ($args.length -ge 3) {
    # parameters must appear pairwise on the command line
    $private:paramCount = $args.length - 3
    if ($paramCount % 2 -ne 0)
    {
        write-error "Arguments for stylesheet parameters must occur pairwise"
        usage
    }

    $private:params = @()
    if ($args.length -gt 3)
    {
        $private:params = $args[3 .. ($args.length - 1)]
    }
    
    runXslt $args[0] $args[1] $args[2] $params
}
elseif ($args.Count -eq 0)
{
    foreach ($run in $runs) {
        runXslt $run[0] $run[1] $run[2] @()
    }
}
else
{
    usage
}

exit 0
