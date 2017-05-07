// BIBLIOTHEQUE WIRES V0.1
{
	var freqRange = Dictionary.newFrom([
		kr: {|f| 2**(f*4+4)}, ar: {|f| 2**(f*3+9)}]);
	var gain = {|a| a+1*0.5};
	var mult = {|a| 2**(gain.(a)*3)};
	var normalizeWeight = {|list, weight = 1| [weight/list.size, list]};
	[
		// CONSTANTE
		normalizeWeight.([[5, [{ DC.kr(Rand(-1, 1)) }, []]]] ++
		// GENERATEURS DE SEQUENCE
			([[Dwhite, [-1, 1]]] ++ (1..4).collect {|n|[Dxrand, [(n.neg..n)/n, inf]]} ++
				(1..4).collect {|n|[Dseq, [(n.neg..n)/n, inf]]}).collect
			{|p1| var class1, args1; #class1, args1 = p1;
				([[Dwhite, [0.2, 4]]] ++ (1..4).collect {|n|[Dxrand, [(1..2*n)/n, inf]]} ++
					(2,2..8).collect {|n|[Dseq, [((1..n-1)++(n..2))/n, inf]]}).collect
				{|p2| var class2, args2; #class2, args2 = p2;
					{DemandEnvGen.kr(class1.performList(\new, args1),
						class2.performList(\new, args2), 0)}
		}}.flat.collect([_,[]])),
		// OSCILLATEURS
		[\kr, \ar].collect {|rate, lvl|
			[SinOsc, LFSaw, LFPulse, LFTri, LFNoise0, LFNoise1].collect {|class|
				[{|amp, freq|
					gain.(amp) * class.perform(rate, freqRange[rate].(freq));
				}, (0..lvl+1)!2]
		}}.collect(normalizeWeight.(_))/*.reduce('++') ++*/,

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
				[{|a,b| a*b}, [[lvl], (1..lvl)]],
				// distorsion
				[{|sig, amt| sig ** (2 ** (amt * 2))}, [[lvl], (0..lvl)]],
				// quantification
				[{|sig, amt| sig.round(mult.(amt).reciprocal)}, [[lvl], (0..lvl)]]
			] ++
			// clip et fold
			[\clip2, \fold2].collect {|op| [{|sig, amt| (sig * mult.(amt)).perform(op)},
				[[lvl], (0..lvl)]]}
		}.collect(normalizeWeight.(_))/*.reduce('++')*/
	]
}.()
