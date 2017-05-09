// BIBLIOTHEQUE WIRES V0.1
{
	var option = {|val, opt, func| if (opt.notNil) {func.(val)} {val} };
	var freqRange = Dictionary.newFrom([
		kr: {|f| 2**(f*4+4)}, ar: {|f| 2**(f*3+9)}]);
	var gain = {|a| a+1*0.5};
	var mult = {|a| 2**(gain.(a)*3)};
	var normalizeWeight = {|list, weight = 1| [weight/list.size, list]};
	var typeWeight = {|type, list| [\type, {|i| (type == i).asInteger + 0.01}, list]};
	[
		// les numéros de types
		[
			control: (0..3), // constante, séquence, oscillateur, fonction arithmétique
			audio: (0..1) // oscillateur, fonction arithmétique
		],
		// CONSTANTE
		typeWeight.(2, [{ DC.kr(Rand(-1, 1)) }, []]),
		// GENERATEURS DE SEQUENCE
		typeWeight.(3, normalizeWeight.(
			([[Dwhite, [-1, 1]]] ++ (1..4).collect {|n|[Dxrand, [(n.neg..n)/n, inf]]} ++
				(1..4).collect {|n|[Dseq, [((n.neg..n-1)++(n..n.neg+1))/n, inf]]})
			.collect {|p1| var class1, args1; #class1, args1 = p1;
				([[Dwhite, [0.125, 1] * mult.(Rand(-1, 1))]] ++
					(1..3).collect {|n|[Dxrand, [(0.5**(0..n)) * mult.(Rand(-1, 1)), inf]]} ++
					(2..4).collect {|n|[Dseq,
						[({|i|(0.5**i)!(2**i)}!n).reduce('++').scramble * mult.(Rand(-1, 1)), inf]]})
				.collect {|p2| var class2, args2; #class2, args2 = p2;
					{|width| gain.(width) * DemandEnvGen.kr(
						class1.performList(\new, args1),
						class2.performList(\new, args2), 0)}
			}}.flat.collect{|f| [f,[(0..1)]]}
		)),
		// OSCILLATEURS
		typeWeight.(0,
			[\kr, \ar].collect {|rate, lvl| normalizeWeight.(
				[SinOsc, LFSaw, LFPulse, LFTri, LFNoise0, LFNoise1].collect {|class|
					[nil, [[2]]].collect {|fmParm|
						[{|amp, freq, fm|
						gain.(amp) * class.perform(rate,
								option.(freqRange[rate].(freq), fmParm, {|it| it * (2 ** fm)}));
						}, (0..1)!2 ++ fmParm]
					}
			}.reduce('++'))}
		),

		// OPERATEURS ARITHMETIQUES
		typeWeight.(1,
			(1..2).collect {|lvl| normalizeWeight.(
				[
					// moyenne
					[{|a, b| (a + b) * 0.5}, [lvl]!2],
					// melange
					[{|a, b, mix| var pos = gain.(mix); (a * (1-pos)) + (b * pos)},
						[lvl]!2 ++ [(0..1)]],
					// produit
					[{|a,b| a*b}, [[lvl], (1..lvl)]],
					// distorsion
					[{|sig, amt| sig ** (2 ** (amt * 2))}, [[lvl], (0..lvl)]],
					// quantification
					[{|sig, amt| sig.round(mult.(amt).reciprocal)}, [[lvl], (0..lvl)]],
					// modulation en anneau
					[{|sig, mod, amt| var a = gain.(amt); (sig * (1 + (mod * a))) / (1 + a)},
						[lvl]!2 ++ [(0..lvl)]]
				] ++
				// clip et fold
				[\clip2, \fold2].collect {|op| [{|sig, amt| (sig * mult.(amt)).perform(op)},
					[[lvl], (0..lvl)]]})
			}
		)
	]
}.()
