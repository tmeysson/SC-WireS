Wires_Def : SynthDef {
	// la version et le contenu de la bibliothèque
	classvar libVersion, libContent;
	// indicateur de mise à jour de la bibliothèque
	classvar libUpdate, libDate;
	// les définitions
	classvar defs;
	// la définition du module de sortie
	classvar <outDef;
	// les SynthDef de transition
	classvar transDefs;
	// les SynthDef de réinjection
	classvar <feedbackDefs;
	// les définitions par défaut
	classvar defaultDefs;

	// la définition et les arguments
	var synthDef, <synthArgs;
	// le type de sortie
	var <rate;
	// le poids relatif et le type
	var <weight;
	var <type;
	// le nombre de fils audio et contrôle
	var <nbSubs;

	*initClass {
		libUpdate = false;
	}

	*setup {
		this.readLib;
		if (libUpdate) {
			this.removeDefs;
			this.makeDefs;
			this.addDefs;
			libUpdate = false;
		};
	}

	// récupérer le contenu de la bibliothèque
	*readLib {|version = "0-1"|
		var libName = "Wires_lib_%.scd".format(version);
		var fileName = Platform.userExtensionDir +/+ "SC-WireS" +/+ "library" +/+ libName;
		var fileDate = if (File.exists(fileName)) {File.mtime(fileName)}
		{ Error("Library file % does not exist".format(fileName)).throw };
		if (((libDate ? 0) < fileDate) || (libVersion != version))
		{
			var src = File(fileName, "r");
			libContent = src.readAllString.interpret;
			src.close;
			"Loaded %".format(libName).postln;
			libVersion = version;
			libDate = Date.getDate.rawSeconds;
			libUpdate = true;
		}
	}

	// compiler les définitions
	*makeDefs {
		// fonction récursive d'évaluation de la bibliothèque
		var rec;
		// initialiser les definitions
		var maxA = 2, maxK = 3;
		defs = Dictionary.newFrom([
			audio: {{List()} ! (maxA+1)} ! (maxK+1),
			control: {List()} ! (maxK+1)
		]);
		// compiler les définitions
		rec = {|elt, prefix, weight, type|
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
						this.new("%-%-%".format(prefix, i, j), def[0], parms, weight, type);
					}
				}
				// cas d'une pondération
				{def[0].isNumber}
				{
					rec.(def[1..], "%-%".format(prefix, i), def[0] * weight, type)
				}
				// cas d'une fonction de type
				// note: on peut ajouter des fonctionnalités en utilisant différents symboles
				{def[0] == \type}
				{
					rec.(def[2..], "%-%".format(prefix, i), weight, def[1])
				}
				// cas récursif (par défaut)
				{ rec.(def, "%-%".format(prefix, i), weight, type) };
			}
			// concatener les résultats
			.reduce('++');
		};

		rec.(libContent, "", 1)
		// ajouter dans la liste correspondante
		.do {|def|
			switch (def.rate)
			{'audio'}   {defs['audio'][def.nbSubs[0]][def.nbSubs[1]].add(def)}
			{'control'} {defs['control'][def.nbSubs[0]].add(def)};
		};

		// définition du module de sortie
		if (outDef.isNil) {outDef = this.out};

		// définition de la transition
		if (transDefs.isNil) {
			transDefs = ['audio', 'control'].collect {|it|
				var rate = switch(it) {'audio'} {\ar} {'control'} {\kr};
				SynthDef("wires-trans-%".format(it).asSymbol, {|out, in1, in2|
					var env = Line.kr(doneAction: 2);
					Out.perform(rate, out, ((1 - env) * In.perform(rate, in1)) +
						(env * In.perform(rate, in2)));
				})
			}
		};

		if (feedbackDefs.isNil) {
			feedbackDefs = [
				SynthDef('wires-feedback', {|out, in|
					Out.ar(out, InFeedback.ar(in));
				}),
				SynthDef('wires-feedback-delay', {|out, in|
					var delay = Rand(0, 4);
					Out.ar(out, DelayN.ar(InFeedback.ar(in), delay, delay));
				})
			];
		};

		if (defaultDefs.isNil) {defaultDefs = this.defaults};
	}

	*addDefs {
		// ajouter les définitions
		defs.do{|type| type.flat.do(_.add)};
		outDef.add;
		transDefs.do(_.add);
		feedbackDefs.do(_.add);
		defaultDefs.do(_.add);
	}

	*removeDefs {
		// supprimer les définitions
		defs.do{|type| type.flat.do(_.remove)};
		if (outDef.notNil) {outDef.remove};
		transDefs.do {|def| SynthDef.removeAt(def.name)};
		feedbackDefs.do {|def| SynthDef.removeAt(def.name)};
	}

	remove { SynthDef.removeAt(name) }

	*new {|idNum, func, parms, weight, type|
		var rate;
		^super.new("wires%".format(idNum).asSymbol,
			{|out|
				// créer le UGenGraph
				var graph = func.(*parms.collect {|p, i| p[1].(i)});
				// enregistrer le type de sortie
				switch (rate = graph.rate)
				{'scalar'}  {rate = 'control'; Out.kr(out, graph)}
				{'control'} {Out.kr(out, graph)}
				{'audio'}   {Out.ar(out, graph)};
			}
		).defInit(rate, parms, weight, type);
	}

	defInit {|rt, parms, wght, tp|
		rate = rt;
		// enregistrer les types des arguments
		synthArgs = parms.collect(_[0]);
		// enregistrer le nombre de fils et le poids
		type = tp;
		weight = wght;
		// enregistrer le nombre de fils audio/contrôle
		nbSubs = if (synthArgs.isEmpty) {[0, 0]}
		{synthArgs.sum {|it| [(it == 'control').asInteger, (it == 'audio').asInteger]}};
	}

	*out {
		if (UGen.findMethod('antiPok').notNil) {
			^super.new('wires-out', {|vol = 0.25, p0, gate = 1|
				Out.ar(0, Pan2.ar(vol * EnvGen.kr(Env.asr(1,1,1), gate, doneAction: 2) * In.ar(p0).antiPok,
					DemandEnvGen.kr(Dwhite(-1, 1), 2 ** Dwhite(0, 6))
				))
			}).outDefInit;
		} {
			^super.new('wires-out', {|vol = 0.25, p0, gate = 1|
				Out.ar(0, Pan2.ar(vol * EnvGen.kr(Env.asr(1,1,1), gate, doneAction: 2) * In.ar(p0),
					DemandEnvGen.kr(Dwhite(-1, 1), 2 ** Dwhite(0, 6))
				))
			}).outDefInit;
		};
	}

	outDefInit {
		synthArgs = ['audio'];
		nbSubs = [0, 1];
	}

	*defaults {
		^Dictionary.newFrom([
			audio: super.new('wires-default-audio', {|out|
				Out.ar(out, Silent.ar)}).defaultDefInit,
			control: super.new('wires-default-control', {|out|
				Out.kr(out, DC.kr(0))}).defaultDefInit
		]);
	}

	defaultDefInit {
		synthArgs = [];
		nbSubs = [0, 0];
	}

	*randDef {|rate, typeWeights, maxK, maxA, maxFB|
		var minA;
		var minK;
		var subSet;
		var weights;
		var res;

		#minA, minK = if(maxA > 0)
		{[1, 0]}
		{[maxFB, if(maxK > 0) {1} {0}]};

		subSet = defs[rate][minK..maxK];
		if (rate == 'audio') {subSet = subSet.collect {|tab| tab[minA..maxA+maxFB]}};

		subSet = subSet.flat;
		weights = subSet.collect {|def| def.weight * typeWeights[def.type]}.normalizeSum;
		res = subSet[weights.windex];
		if (res.notNil) {^res}
		{
			"WARNING: no Wires_Def of signature [%, %, %]".format(maxK, maxA, maxFB).postln;
			^defaultDefs[rate];
		};
	}
}
