// BIBLIOTHEQUE WIRES V0.1
{
	var freqRange = Dictionary.newFrom([
		kr: {|f| 2**(f*4+4)}, ar: {|f| 2**(f*3+9)}]);
	var gain = {|a| a+1*0.5};
	var mult = {|a| 2**(gain.(a)*3)};
	// OSCILLATEURS
	[SinOsc, LFSaw, LFPulse, LFTri, LFNoise0, LFNoise1].collect {|class|
		[\kr, \ar].collect {|rate, lvl|
			[{|amp, freq|
				gain.(amp) * class.perform(rate, freqRange[rate].(freq));
			}, 2, lvl+1]
	}}.reduce('++') ++

	// BRUIT FILTRE
	[WhiteNoise, PinkNoise, BrownNoise].collect {|class|
		[\kr, \ar].collect {|rate, lvl|
			[{|amp, freq, q| gain.(amp) * BPF.perform(rate, class.perform(rate),
				freqRange[rate].(freq), mult.(q).reciprocal)}, 3, lvl+1]
	}}.reduce('++') ++

	// OPERATEURS ARITHMETIQUES
	[
		// moyenne
		[{|a, b, mix| var pos = gain.(mix); (a * (1-pos)) + (b * pos)}, 3, 2],
		// produit
		[{|a,b| a*b}, 2, 2],
		// distorsion
		[{|sig, amt| sig ** (2 ** (amt * 2))}, 2, 2],
		// quantification
		[{|sig, amt| sig.round(mult.(amt).reciprocal)}, 2, 2],
	] ++
	// clip et fold
	[\clip2, \fold2].collect {|op| [{|sig, amt| (sig * mult.(amt)).perform(op)}, 2, 2]}
}.()