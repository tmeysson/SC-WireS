// BIBLIOTHEQUE WIRES V0.1
{
	var freqRange = Dictionary.newFrom([
		kr: {|f| 2**(f*4+4)}, ar: {|f| 2**(f*3+9)}]);
	var gain = {|a| a+1*0.5};
	var mult = {|a| 2**(gain.(a)*3)};
	var normalizeWeight = {|list, weight = 1| [weight/list.size, list]};
	[
		// CONSTANTE
		[1, [{ DC.kr(Rand(-1, 1)) }, []]], normalizeWeight.(
		// GENERATEURS DE SEQUENCE
			([[Dwhite, [-1, 1]]] ++ (1..4).collect {|n|[Dxrand, [(n.neg..n)/n, inf]]} ++
				(1..4).collect {|n|[Dseq, [((n.neg..n-1)++(n..n.neg+1))/n, inf]]}).collect
			{|p1| var class1, args1; #class1, args1 = p1;
				([[Dwhite, [0.125, 1]]] ++ (1..3).collect {|n|[Dxrand, [0.5**(0..n), inf]]} ++
					(2..4).collect {|n|[Dseq,
						[({|i|(0.5**i)!(2**i)}!n).reduce('++').scramble, inf]]}).collect
				{|p2| var class2, args2; #class2, args2 = p2;
					{|width, period| DemandEnvGen.kr(
						gain.(width) * class1.performList(\new, args1),
						mult.(period) * class2.performList(\new, args2), 0)}
		}}.flat.collect{|f| [f,(0..1)!2]}),
		// OSCILLATEURS
		[\kr, \ar].collect {|rate, lvl|
			[SinOsc, LFSaw, LFPulse, LFTri, LFNoise0, LFNoise1].collect {|class|
				[{|amp, freq|
					gain.(amp) * class.perform(rate, freqRange[rate].(freq));
				}, (0..lvl+1)!2]
		}}.collect(normalizeWeight.(_))/*.reduce('++') ++*/,

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
