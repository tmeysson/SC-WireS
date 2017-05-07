Wires_Node {
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
	// le nombre de noeuds du sous-graphe
	var <numNodes;

	*new {|rate = 'audio', depth = 0, target|
		^super.new.nodeInit(rate, depth, target);
	}

	nodeInit {|rate, dpth, target|
		// la définition
		var def;
		// les arguments
		var args;
		// profondeur
		depth = dpth;
		// obtenir une définition aléatoire
		def = Wires_Def.randDef(rate, dpth);
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
		// libérer le Bus à la fin
		synth.onFree {outBus.free};
	}

	*out {
		^super.new.outNodeInit;
	}

	outNodeInit {
		var input;
		depth = -1;
		// créer le groupe d'accueil
		group = Group();
		// créer le sous-groupe
		subGroup = ParGroup(group);
		// créer l'entrée
		input = Wires_Node('audio', 0, subGroup);
		subNodes = [[in: input]];
		numNodes = input.numNodes + 1;
		// créer le Synth
		synth = Wires_Def.outDef.makeInstance([in: input.outBus], group);
		// libérer le groupe à la fin
		synth.onFree {group.free};
	}

	renew {|num|
		var select = subNodes.choose;
		var node = select[1];
		if (node.numNodes <= num)
		// remplacer le noeud
		{
			var new = Wires_Node(node.outBus.rate, depth + 1, subGroup);
			synth.set(select[0], new.outBus);
			select[1] = new;
			numNodes = numNodes - node.numNodes + new.numNodes;
			node.free;
		}
		// propager la requête
		{
			numNodes = numNodes - node.numNodes;
			node.renew(num);
			numNodes = numNodes + node.numNodes;
		}
	}

	free {
		if (group.notNil) {group.free} {synth.free};
	}

	release {
		synth.release;
	}
}
