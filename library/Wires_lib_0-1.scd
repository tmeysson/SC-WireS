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
			}, (0..lvl+1)!2]
	}}.reduce('++') ++

	/*
	// BRUIT FILTRE
	[WhiteNoise, PinkNoise, BrownNoise].collect {|class|
		[\kr, \ar].collect {|rate, lvl|
			[{|amp, freq, q| gain.(amp) * BPF.perform(rate, class.perform(rate),
				freqRange[rate].(freq), mult.(q).reciprocal)}, (0..1)!3]
	}}.reduce('++') ++
	*/

	// OPERATEURS ARITHMETIQUES
	// moyenne
	(1..2).collect {|lvl|
		[
			[{|a, b| (a + b) * 0.5}, [lvl]!2],
			// melange
			[{|a, b, mix| var pos = gain.(mix); (a * (1-pos)) + (b * pos)}, [lvl]!2 ++ [(0..1)]],
			// produit
			[{|a,b| a*b}, [lvl]!2],
			// distorsion
			[{|sig, amt| sig ** (2 ** (amt * 2))}, [[lvl], (0..lvl)]],
			// quantification
			[{|sig, amt| sig.round(mult.(amt).reciprocal)}, [[lvl], (0..lvl)]]
		] ++
		// clip et fold
		[\clip2, \fold2].collect {|op| [{|sig, amt| (sig * mult.(amt)).perform(op)},
			[[lvl], (0..lvl)]]}
	}.reduce('++')
}.()
