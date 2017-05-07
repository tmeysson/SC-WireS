Wires_Def {
	// la version et le contenu de la bibliothèque
	classvar libVersion, libContent;
	// les définitions
	classvar defs;
	// la définition du module de sortie
	classvar <outDef, volume;
	// les poids
	classvar weights;
	// le nombre de niveaux par cible d'espérance
	classvar nbLevels;

	// la définition et les arguments
	var synthDef, <synthArgs;
	// le type de sortie
	var <rate;

	// récupérer le contenu de la bibliothèque
	*readLib {|major = 0, minor = 1, reload = false|
		if (reload || (libVersion != [major, minor]))
		{
			var src = File(Platform.userExtensionDir +/+ "Wires" +/+ "library" +/+
				"Wires_lib_%-%.scd".format(major, minor), "r");
			libContent = src.readAllString.interpret;
			src.close;
			libVersion = [major, minor];
		}
	}

	// compiler les définitions
	*makeDefs {|reload = false|
		// initialiser les definitions
		defs = Dictionary.newFrom([audio: List(), control: List()]);
		// lire la bibliothèque si nécessaire
		this.readLib(reload: reload);
		// compiler les définitions
		// pour chaque définition
		libContent.collect {|def, i|
			// énumérer les combinaisons de paramètres
			([
				['scalar', { Rand(-1, 1) }],
				['control', {|n| In.kr("p%".format(n).asSymbol.kr) }],
				['audio', {|n| In.ar("p%".format(n).asSymbol.kr) }]
			][..def[2]] ! def[1]).allTuples
			// pour chaque combinaison
			.collect {|parms, j|
			// créer la définition
				this.new(i, j, def[0], parms);
			}
		}
		// concatener les résultats
		.reduce('++')
		// ajouter dans la liste correspondante
		.do {|def| defs[def.rate].add(def) };

		// définition du module de sortie
		volume = 0.5;
		outDef = this.out;
	}

	*addDefs {
		// ajouter les définitions
		defs.do(_.do(_.add));
		outDef.add;
	}

	add { synthDef.add }

	// compiler les poids
	*makeWeights {|start = 1.5, factor = (2/3), nblvls = 3|
		// listes des nombre de fils
		var sons = defs.collect {|defList|
			defList.collect {|def|
					def.synthArgs.inject(0) {|acc, it| acc + (it != 'scalar').asInteger}
			}
		};
		// fonctions de poids
		var weightFuncs = sons.collect {|sonsList|
			{|alpha|
				((sonsList + 1) ** alpha.neg).normalizeSum
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

	*new {|defNum, subNum, func, parms|
		^super.new.defInit(defNum, subNum, func, parms);
	}

	defInit {|defNum, subNum, func, parms|
		// créer la SynthDef
		synthDef = SynthDef("wires-%-%".format(defNum, subNum).asSymbol,
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
	}

	*out {
		^super.new.outDefInit;
	}

	outDefInit {
		synthDef = SynthDef('wires_out', {|in, gate = 1|
			Out.ar(0, (volume * EnvGen.kr(Env.asr(1,1,1), gate, doneAction: 2) * In.ar(in)) ! 2)
		});
		synthArgs = ['audio'];
	}

	*randDef {|rate, depth|
		// choisir une définition suivant les poids et la profondeur
		^defs[rate][weights[rate][(depth/nbLevels).floor.clip(0,2)].windex];
	}

	makeInstance {|args, target|
		^Synth(synthDef.name, args, target, 'addToTail');
	}
}