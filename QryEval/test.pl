#!/usr/bin/perl -w

#use strict;
use Spreadsheet::ParseExcel;

my $parser   = Spreadsheet::ParseExcel->new();
my $workbook = $parser->parse('hw4.xls');

if ( !defined $workbook ) {
    die $parser->error(), ".\n";
}

for my $worksheet ( $workbook->worksheets() ) {

    my ( $row_min, $row_max ) = $worksheet->row_range();
    my ( $col_min, $col_max ) = $worksheet->col_range();

    for my $row ( $row_min .. $row_max ) {
        for my $col ( $col_min .. $col_max ) {

            my $cell = $worksheet->get_cell( $row, $col );
            next unless $cell;

            my $format = $cell->get_format();

            print "Row, Col    = ($row, $col)\n";
            print "Value       = ", $cell->value(), "\n";
            print "Indent      = ", $format->{Indent}, "\n";

            print "\n";
        }
    }
}


#!/usr/bin/perl

use strict;
use warnings;

use Spreadsheet::ParseExcel;
use Spreadsheet::ParseExcel::SaveParser;


# Open an existing file with SaveParser
my $parser   = Spreadsheet::ParseExcel::SaveParser->new();
my $template = $parser->Parse('template.xls');


# Get the first worksheet.
my $worksheet = $template->worksheet(0);
my $row  = 0;
my $col  = 0;


# Overwrite the string in cell A1
$worksheet->AddCell( $row, $col, 'New string' );


# Add a new string in cell B1
$worksheet->AddCell( $row, $col + 1, 'Newer' );


# Add a new string in cell C1 with the format from cell A3.
my $cell = $worksheet->get_cell( $row + 2, $col );
my $format_number = $cell->{FormatNo};

$worksheet->AddCell( $row, $col + 2, 'Newest', $format_number );


# Write over the existing file or write a new file.
$template->SaveAs('newfile.xls');
