Wires_Node {
	// la définition et les arguments
	var def, args;
	// le Synth et son état
	var synth, isRunning;
	// les sous-noeuds
	var subNodes;
	// le Group
	var group;
	// le groupe des sous-noeuds
	var subGroup;
	// le Bus de sortie
	var <outBus;
	// la profondeur
	var depth;
	// le poids des types
	var typeWeights;
	// le quota de noeuds
	var <quota;
	// le nombre de noeuds du sous-graphe
	var <numNodes;
	// la date de création
	var <date;
	// le verrou de renouvellement
	var lock;
	// le niveau de variable ciblé
	var varLevel;

	*new {|typeWeights, quota|
		^super.new.nodeInit(typeWeights, quota);
	}

	nodeInit {|tWghts, qt|
		// date
		date = Date.getDate.rawSeconds;
		// poids des types
		typeWeights = tWghts;
		// le quota de noeuds
		quota = qt;
		// activer le noeud
		isRunning = true;
		// initialiser le verrou
		lock = Semaphore();
	}

	makeArgs {|def, target|
		var subQt;
		// calculer les quotas des sous-noeuds
		if (def.synthArgs.notEmpty) {
			subQt = (
				[(quota - def.nbSubs)] ++
				def.synthArgs.collect {|rate|
					switch (rate)
					{'scalar'} {[0, 0]}
					{'control'} {[rand(1.0), 0]}
					{'audio'}   {[rand(2.0), rand(1.0)]};
				}
			).flop.collect {
				|it| var a, b; #a ... b = it;
				if (b.any(_!=0)) {
					(b.normalizeSum * a).integrate.round.differentiate.asInteger
				} {b}
			}.flop
		};
		// créer les arguments
		args = def.synthArgs.collect {|rate, i|
			if (rate != 'scalar') {[i, rate, subQt[i]]};
		}.select(_.notNil);
		// créer les sous-noeuds
		if (args.notEmpty) {
			// créer le groupe d'accueil
			group = Group(target);
			// créer un groupe des sous-noeuds
			subGroup = ParGroup(group);
			args = args.collect {|item|
				var i, rate, qt;
				#i, rate, qt = item;
				["p%".format(i).asSymbol,
					Wires_InnerNode(rate, depth + 1, subGroup, varLevel, typeWeights, this, qt)]
			};
			// enregistrer les sous-noeuds
			subNodes = args;
			// obtenir les Bus de sortie
			args = args.collect {|e| [e[0], e[1].outBus] };
			// aplatir la liste
			args = args.reduce('++');
		} {
			// il n'y a pas de sous-noeuds
			subNodes = [];
		};
		numNodes = 1 + subNodes.sum {|e| e[1].numNodes};
	}

	makeSynth {|target|
		synth = Synth(def.name, args, target, 'addToTail');
	}

	renew {|num|
		var index, select, node;
		{
			// section vérouillée
			lock.wait;
			if (num < 1) {num = 1};
			index = subNodes.minIndex {|it| it[1].date(this) + rand(1.0)};
			select = subNodes[index];
			node = select[1];
			if (node.numNodes <= num)
			// remplacer le noeud
			{
				// effectuer la transition
				var rate, new, bus;
				rate = node.outBus.rate;
				subNodes[index][1] = new = Wires_InnerNode(rate, depth + 1, subGroup, varLevel, typeWeights,
					this, node.quota(this));
				bus = Bus.alloc(rate);
				Synth("wires-trans-%".format(rate).asSymbol,
					[out: bus, in1: node.outBus, in2: new.outBus], synth, 'addBefore').onFree {bus.free};
				synth.set(select[0], bus);
				numNodes = numNodes - node.numNodes + new.numNodes;
				// attendre la fin de la transition
				1.wait;
				// terminer la transition
				synth.set(select[0], new.outBus);
				node.free;
			}
			// propager la requête
			{
				numNodes = numNodes - node.numNodes;
				node.renew(num);
				numNodes = numNodes + node.numNodes;
			};
			// fin du verrou
			lock.signal;
		}.forkIfNeeded;
	}

	free {|parent|
		{
			// section vérouillée
			lock.wait;
			if (isRunning) {synth.free; isRunning = false};
			if (outBus.notNil) {outBus.free};
			subNodes.do {|node| node[1].free(this) };
			if (subGroup.notNil) {subGroup.free};
			if (group.notNil) {group.free};
			// fin du verrou
			lock.signal;
		}.forkIfNeeded;
	}

	release {
		synth.release;
	}

	countNodes {|coeff = 1, update = false|
		var count = subNodes.sum {|n| n[1].countNodes(coeff, update)} + coeff;
		if (update) {numNodes = count};
		^count;
	}
}
