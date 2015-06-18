use strict;

# Check usage:
if($#ARGV != 2)
{
	print STDERR "Usage: perl $0 <num-src-labels> <num-tgt-labels> <pct-coverage>\n";
	exit(1);
}

# Get parameters:
my ($numSrc, $numTgt, $pctCoverage) = @ARGV;

# Generate fake, randomly drawn label alignment counts:
foreach my $s (1..$numSrc)
{
	foreach my $t (1..$numTgt)
	{
		next if(int(rand(100)) >= $pctCoverage);
		my $c = int(rand(10000));
		print "$s\t$t\t$c\n";
	}
}
