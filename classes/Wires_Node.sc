Wires_Node {
	// le groupe parallèle de base
	classvar baseGroup;

	// le Synth
	var synth;
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
	// le nombre de noeuds du sous-graphe
	var <numNodes;
	// la date de création
	var <date;

	*new {|rate = 'audio', depth = 0, target, typeWeights|
		^super.new.nodeInit(rate, depth, target, typeWeights);
	}

	nodeInit {|rate, dpth, target, tWghts|
		// la définition
		var def;
		// les arguments
		var args;
		// date
		date = Date.getDate.rawSeconds;
		// profondeur
		depth = dpth;
		// poids des types
		typeWeights = tWghts;
		// obtenir une définition aléatoire
		def = Wires_Def.randDef(rate, dpth, typeWeights);
		// créer le Bus de sortie
		outBus = Bus.alloc(rate);
		// créer les arguments
		args = def.synthArgs.collect {|rate, i|
			if (rate != 'scalar')
			{[i, rate]}
		}.select(_.notNil);
		// créer les sous-noeuds
		if (args.notEmpty) {
			// créer le groupe d'accueil
			group = Group(target);
			// créer un groupe des sous-noeuds
			subGroup = ParGroup(group);
			args = args.collect {|item|
				var i, rate;
				#i, rate = item;
				["p%".format(i).asSymbol, Wires_Node(rate, depth + 1, subGroup)]
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
	}

	*out {|volume = 0.25, typeWeights|
		^super.new.outNodeInit(volume, typeWeights);
	}

	outNodeInit {|volume, tWghts|
		// date
		date = Date.getDate.rawSeconds;
		// profondeur
		depth = -1;
		// poids des types
		typeWeights = tWghts;
		// créer le groupe de base, si il n'existe pas
		if (baseGroup.isNil) {baseGroup = ParGroup()};
		// créer le groupe d'accueil
		group = Group(baseGroup);
		// créer le sous-groupe
		subGroup = ParGroup(group);
		// créer l'entrée
		subNodes = [[in: Wires_Node('audio', 0, subGroup, typeWeights)],
			[pos: Wires_Node('control', 0, subGroup, typeWeights)]];
		numNodes = subNodes.sum {|e| e[1].numNodes } + 1;
		// créer le Synth
		synth = Wires_Def.outDef.makeInstance([vol: volume] ++
			subNodes.collect {|p| [p[0], p[1].outBus]}.reduce('++'),
			group);
		// libérer le sous-graphe à la fin
		synth.onFree {this.free};
	}

	renew {|num|
		var index = subNodes.minIndex {|it| it[1].date};
		var select = subNodes[index];
		var node = select[1];
		if (node.numNodes <= num)
		// remplacer le noeud
		{
			var rate = node.outBus.rate;
			var new = Wires_Node(rate, depth + 1, subGroup, typeWeights);
			// effectuer la transition
			Routine {
				var bus = Bus.alloc(rate);
				var trans = Synth("wires-trans-%".format(rate).asSymbol,
					[out: bus, in1: node.outBus, in2: new.outBus], synth, 'addBefore');
				synth.set(select[0], bus);
				1.wait;
				synth.set(select[0], new.outBus);
				node.free; bus.free;
			}.play;
			subNodes[index][1] = new;
			numNodes = numNodes - node.numNodes + new.numNodes;
		}
		// propager la requête
		{
			numNodes = numNodes - node.numNodes;
			node.renew(num);
			numNodes = numNodes + node.numNodes;
		}
	}

	free {
		if (synth.isRunning) {synth.free};
		if (outBus.notNil) {outBus.free};
		subNodes.do {|node| node[1].free };
		if (subGroup.notNil) {subGroup.free};
		if (group.notNil) {group.free};
	}

	release {
		synth.release;
	}
}
