Wires_Node {
	// le Synth
	var synth;
	// les sous-noeuds
	var subNodes;
	// le Group
	var group;
	// le Bus de sortie
	var <outBus;
	// la profondeur
	var depth;

	*new {|rate = 'audio', depth = 0, target|
		^super.new.nodeInit(rate, depth, target);
	}

	nodeInit {|rate, dpth, target|
		// la définition
		var def;
		// le groupe des sous-noeuds
		var subGroup;
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
			args = args.flop;
			subNodes = args[1];
			// obtenir les Bus de sortie
			args = [args[0], args[1].collect(_.outBus)].flop;
			// aplatir la liste
			args = args.reduce('++');
		} {
			// il n'y a pas de sous-noeuds
			subNodes = [];
		};
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
		var subGroup, input;
		// créer le groupe d'accueil
		group = Group();
		// créer le sous-groupe
		subGroup = ParGroup(group);
		// créer l'entrée
		input = Wires_Node('audio', 0, subGroup);
		subNodes = [input];
		// créer le Synth
		synth = Wires_Def.outDef.makeInstance([in: input.outBus], group);
		// libérer le Bus à la fin
		synth.onFree {this.free};
	}

	free {
		if (group.notNil) {group.free};
	}

	release {
		synth.release;
	}
}
