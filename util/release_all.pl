#!/usr/bin/env perl™

use strict;
use warnings;
use feature 'say';

foreach my $version (grep {/\w+/} map {/spark\-(\d\.\d\.\d)/;$1 || ""} qx/git branch -l/) {
	my $sh = <<SHELL
        git checkout spark-$version
        sbt +publishSigned
SHELL
;
	say qx{$sh}
}
