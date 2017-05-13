Wires_Node {
	// le groupe parallèle de base
	classvar baseGroup;

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
	var <>lock;
	// le niveau de variable ciblé
	var varLevel;

	*new {|rate = 'audio', depth = 0, target, varLevel = 0, typeWeights, parent, quota, isVar = false|
		if (isVar.not && 0.1.coin)
		{ ^Wires_Var.getVar(rate, depth, target, varLevel, typeWeights, parent, quota) }
		{ ^super.new.nodeInit(rate, depth, target, varLevel, typeWeights, quota) };
	}

	nodeInit {|rate, dpth, target, level, tWghts, qt|
		// la définition
		var def;
		// les arguments
		var args;
		// les quotas des sous-noeuds
		var subQt;
		// date
		date = Date.getDate.rawSeconds;
		// profondeur
		depth = dpth;
		// niveau de variable
		varLevel = level;
		// poids des types
		typeWeights = tWghts;
		// le quota de noeuds
		quota = qt;
		// obtenir une définition aléatoire
		def = Wires_Def.randDef(rate, typeWeights, *quota);
		// créer le Bus de sortie
		outBus = Bus.alloc(rate);
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
					Wires_Node(rate, depth + 1, subGroup, varLevel, typeWeights, this, qt)]
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
		// ajouter le Bus de sortie
		args = [out: outBus] ++ args;
		// créer le Synth
		synth = def.makeInstance(args, group ? target);
		isRunning = true;
		lock = false;
	}

	*out {|volume = 0.25, typeWeights, quota|
		^super.new.outNodeInit(volume, typeWeights, quota);
	}

	outNodeInit {|volume, tWghts, qt|
		// quotas des sous-noeuds
		var subQt;
		// date
		date = Date.getDate.rawSeconds;
		// profondeur
		depth = -1;
		// quota
		quota = qt;
		// répartition du quota
		subQt = [([rand(0.5), 1] * quota[0]).round.differentiate.asInteger, [0, quota[1]]].flop;
		// niveau de variable
		varLevel = 0;
		// poids des types
		typeWeights = tWghts;
		// créer le groupe de base, si il n'existe pas
		if (baseGroup.isNil) {baseGroup = ParGroup()};
		// créer le groupe d'accueil
		group = Group(baseGroup);
		// créer le sous-groupe
		subGroup = ParGroup(group);
		// créer l'entrée
		subNodes = [[in: Wires_Node('audio', 0, subGroup, 0, typeWeights, this, subQt[1])],
			[pos: Wires_Node('control', 0, subGroup, 0, typeWeights, this, subQt[0])]];
		numNodes = subNodes.sum {|e| e[1].numNodes } + 1;
		// créer le Synth
		synth = Wires_Def.outDef.makeInstance([vol: volume] ++
			subNodes.collect {|p| [p[0], p[1].outBus]}.reduce('++'),
			group);
		// libérer le sous-graphe à la fin
		synth.onFree {isRunning = false; this.free};
		isRunning = true;
	}

	renew {|num|
		var index, select, node;
		if (num < 1) {num = 1};
		index = subNodes.minIndex {|it| it[1].date + rand(1.0) +
			// ne pas sélectionner le noeuds vérouillés
			(it[1].lock.asInteger * 1e12)};
		select = subNodes[index];
		node = select[1];
		// si le noeud est déverouillé
		if (node.lock.not) {
			if (node.numNodes <= num)
			// remplacer le noeud
			{
				// vérouiller le noeud
				node.lock = true;
				// effectuer la transition
				Routine {
					var rate = node.outBus.rate;
					var new = Wires_Node(rate, depth + 1, subGroup, varLevel, typeWeights,
						this, node.quota(this));
					var bus = Bus.alloc(rate);
					numNodes = numNodes + new.numNodes;
					Synth("wires-trans-%".format(rate).asSymbol,
						[out: bus, in1: node.outBus, in2: new.outBus], synth, 'addBefore').onFree {bus.free};
					synth.set(select[0], bus);
					1.wait;
					synth.set(select[0], new.outBus);
					node.free;
					subNodes[index][1] = new;
					numNodes = numNodes - node.numNodes;
				}.play;
			}
			// propager la requête
			{
				numNodes = numNodes - node.numNodes;
				node.renew(num);
				numNodes = numNodes + node.numNodes;
			}
		}
	}

	free {|parent|
		if (isRunning) {synth.free; isRunning = false};
		if (outBus.notNil) {outBus.free};
		subNodes.do {|node| node[1].free(this) };
		if (subGroup.notNil) {subGroup.free};
		if (group.notNil) {group.free};
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
