#!/usr/bin/perl

#
#  This perl script illustrates fetching information from a CGI program
#  that typically gets its data via an HTML form using a POST method.
#
#  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
#
use Spreadsheet::ParseExcel::SaveParser;
use LWP::Simple;
use Spreadsheet::WriteExcel;
# sudo cpan Spreadsheet::ParseExcel
#use Spreadsheet::ParseExcel;
use List::Util qw(first);

#my $fileIn = 'Indri-Bow.teIn';
my $fileIn = 'bm25.trec_eval.txt';
my $url = 'http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi';

#  Fill in your USERNAME and PASSWORD below.

my $ua = LWP::UserAgent->new();
   $ua->credentials("boston.lti.cs.cmu.edu:80", "HTS", "sxu1", "zhuxianlian1");
my $result = $ua->post($url,
		       Content_Type => 'form-data',
		       Content      => [ logtype => 'Summary',	# cgi parameter
					 infile => [$fileIn],	# cgi parameter
					 hwid => 'HW4'		# cgi parameter
		       ]);



my $result = $result->as_string;	# Reformat the result as a string
   $result =~ s/<br>/\n/g;		# Replace <br> with \n for clarity


#my @data=split(/map/);
@data=split(/\s+/,$result);
# find the first index of array value
my $map = first { $data[$_] eq "map" } 0 .. $#data;
my $P10=first { $data[$_] eq "P10" } 0 .. $#data;
my $P20=first { $data[$_] eq "P20" } 0 .. $#data;
my $P30=first { $data[$_] eq "P30" } 0 .. $#data;
#print $data[$map+2];
#print exists$data{"P10"};



# Open an existing file with SaveParser
my $parser   = Spreadsheet::ParseExcel::SaveParser->new();
my $workbook = $parser->Parse('hw4.xls');
if ( !defined $workbook ) {
    die $parser->error(), ".\n";
}
my $worksheet= $workbook->worksheet(0);

my ( $row_min, $row_max ) = $worksheet->row_range();
my ( $col_min, $col_max ) = $worksheet->col_range();

print "Row       =($row_min,$row_max)\n";
print "Col       =($col_min,$col_max)\n";
# Add cells
$worksheet->AddCell(0,$col_max+1, $data[$P10+2]);
$worksheet->AddCell(1,$col_max+1, $data[$P20+2]);
$worksheet->AddCell(2,$col_max+1, $data[$P30+2]);
$worksheet->AddCell(3,$col_max+1, $data[$map+2]);

# Write over the existing file
$workbook->SaveAs('hw4.xls');

# create a new excel file
# my $workbook = Spreadsheet::WriteExcel->new('hw4.xls');

# create a new sheet
#$worksheet = $workbook->add_worksheet();

# write to the sheet
#$workbook->close();

#print $result;

exit;
