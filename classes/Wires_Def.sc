Wires_Def {
	// la version et le contenu de la bibliothèque
	classvar libVersion, libContent;
	// indicateur de mise à jour de la bibliothèque
	classvar libUpdate;
	// les définitions
	classvar defs;
	// la définition du module de sortie
	classvar <outDef, volume;
	// les poids
	classvar weights;
	// le nombre de niveaux par cible d'espérance
	classvar nbLevels;
	// les SynthDef de transition
	classvar transDefs;

	// la définition et les arguments
	var synthDef, <synthArgs;
	// le type de sortie
	var <rate;
	// le nombre de fils et le poids relatif
	var <nbSons, <weight;

	*initClass {
		libUpdate = false;
	}

	*setup {|reload, vol = 0.25|
		volume = vol;
		this.readLib(reload: reload);
		if (libUpdate) {
			this.makeDefs;
			this.makeWeights;
			this.addDefs;
			libUpdate = false;
		};
	}

	// récupérer le contenu de la bibliothèque
	*readLib {|major = 0, minor = 1, reload = false|
		if (reload || (libVersion != [major, minor]))
		{
			var src = File(Platform.userExtensionDir +/+ "SC-WireS" +/+ "library" +/+
				"Wires_lib_%-%.scd".format(major, minor), "r");
			libContent = src.readAllString.interpret;
			src.close;
			libVersion = [major, minor];
			libUpdate = true;
		}
	}

	// compiler les définitions
	*makeDefs {|reload = false|
		// fonction récursive d'évaluation de la bibliothèque
		var rec;
		// initialiser les definitions
		defs = Dictionary.newFrom([audio: List(), control: List()]);
		// compiler les définitions
		rec = {|elt, prefix, weight|
			// pour chaque définition
			elt.collect {|def, i|
				case
				// cas terminal
				{def[0].isFunction}
				{
					// énumérer les combinaisons de paramètres
					({|k|
						[
							['scalar', { Rand(-1, 1) }],
							['control', {|n| In.kr("p%".format(n).asSymbol.kr) }],
							['audio', {|n| In.ar("p%".format(n).asSymbol.kr) }]
						][def[1][k]]
					} ! def[1].size).allTuples
					// pour chaque combinaison
					.collect {|parms, j|
						// créer la définition
						this.new("%-%-%".format(prefix, i, j), def[0], parms, weight);
					}
				}
				// cas d'une pondération
				{def[0].isNumber}
				{
					rec.(def[1..], "%-%".format(prefix, i), def[0] * weight)
				}
				// cas récursif (par défaut)
				{ rec.(def, "%-%".format(prefix, i), weight) };
			}
			// concatener les résultats
			.reduce('++');
		};

		rec.(libContent, "", 1)
		// ajouter dans la liste correspondante
		.do {|def| defs[def.rate].add(def) };

		// définition du module de sortie
		outDef = this.out;

		// définition de la transition
		transDefs = ['audio', 'control'].collect {|it|
			var rate = switch(it) {'audio'} {\ar} {'control'} {\kr};
			SynthDef("wires-trans-%".format(it).asSymbol, {|out, in1, in2|
				var env = Line.kr(doneAction: 2);
				Out.perform(rate, out, ((1 - env) * In.perform(rate, in1)) +
					(env * In.perform(rate, in2)));
			})
		};
	}

	*addDefs {
		// ajouter les définitions
		defs.do(_.do(_.add));
		outDef.add;
		transDefs.do(_.add);
	}

	add { synthDef.add }

	// compiler les poids
	*makeWeights {|start = 1.5, factor = (2/3), nblvls = 3|
		// listes des [nombre de fils, poids relatif]
		var defSpecs = defs.collect(_.collect {|def| [def.nbSons, def.weight]});
		var sons = defSpecs.collect(_.collect(_[0]));
		// fonctions de poids
		var weightFuncs = defSpecs.collect {|specList|
			var sonsList, weightList;
			#sonsList, weightList = specList.flop;
			{|alpha|
				(weightList * ((sonsList + 1) ** alpha.neg)).normalizeSum
			}
		};

		// liste des cibles en ordre decroissant
		var targets = (start * (factor ** (0..2)));

		// initialiser le nombre de niveaux
		nbLevels = nblvls;
		// initialiser les poids
		weights = Dictionary.new;
		// on itère sur les types
		sons.keysValuesDo {|rate, list|
			// recherche par interpolation
			var func = weightFuncs[rate];
			var alpha = 0;
			var diff;
			weights[rate] =
			// pour chaque valeur cible
			targets.collect {|target|
				var step = 0.5;
				// si alpha est trop faible, l'augmenter
				while { ((list * func.(alpha)).sum - target) > 0 }
				{ alpha = alpha + 1 };
				// si alpha est trop fort, le diminuer
				while { ((list * func.(alpha)).sum - target) < 0 }
				{ alpha = alpha - 1 };
				// on se trouve en dessous de la valeur cible, à une distance <= 1
				// procéder par dichotomie jusqu'à satisfaction
				while {
					diff = (list * func.(alpha)).sum - target;
					diff.abs > 0.01
				}
				{ alpha = alpha + (step * diff.sign); step = step * 0.5; };
				// retourner la liste de poids
				func.(alpha);
			};
		}
	}

	*new {|idNum, func, parms, weight|
		^super.new.defInit(idNum, func, parms, weight);
	}

	defInit {|idNum, func, parms, wght|
		// créer la SynthDef
		synthDef = SynthDef("wires%".format(idNum).asSymbol,
			{|out|
				// créer le UGenGraph
				var graph = func.(*parms.collect {|p, i| p[1].(i)});
				// enregistrer le type de sortie
				switch (rate = graph.rate)
				{'scalar'}  {rate = 'control'; Out.kr(out, graph)}
				{'control'} {Out.kr(out, graph)}
				{'audio'}   {Out.ar(out, graph)};
			}
		);
		// enregistrer les types des arguments
		synthArgs = parms.collect(_[0]);
		// enregistrer le nombre de fils et le poids
		nbSons = synthArgs.inject(0) {|acc, it| acc + (it != 'scalar').asInteger};
		weight = wght;
	}

	*out {
		^super.new.outDefInit;
	}

	outDefInit {
		synthDef = SynthDef('wires_out', {|in, pos, gate = 1|
			Out.ar(0, Pan2.ar(volume * EnvGen.kr(Env.asr(1,1,1), gate, doneAction: 2) * In.ar(in),
				In.kr(pos)))
		});
		synthArgs = ['audio', 'control'];
	}

	*randDef {|rate, depth|
		// choisir une définition suivant les poids et la profondeur
		^defs[rate][weights[rate][(depth/nbLevels).floor.clip(0,2)].windex];
	}

	makeInstance {|args, target|
		^Synth(synthDef.name, args, target, 'addToTail');
	}
}
