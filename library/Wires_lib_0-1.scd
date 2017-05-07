// BIBLIOTHEQUE WIRES V0.1
[SinOsc].collect {|class| [\kr, \ar].collect {|rate, lvl|
	[{|amp, freq|
		var fRange = Dictionary.newFrom([kr: 3, ar: 9]);
		(0.5 * (amp + 1)) * class.perform(rate, 2 ** (fRange[rate] + (freq * 3)));
	}, 2, lvl+1]
}}.reduce('++') ++
[
]
