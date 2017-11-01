// CLASSE D'INTERFACE DE WIRES
Wires {
	var out;
	var loop;

	*new {|volume = 0, nbAudio = 5, nbControl = 10, typeWeights = #[1,1,1,1]|
		^super.new.wiresInit(volume, nbAudio, nbControl, typeWeights);
	}

	wiresInit {|volume, nbAudio, nbControl, typeWeights|
		loop = Routine {
			Server.default.bootSync;
			Wires_Def.setup;
			// Wires_InnerNode.makeDummies;
			Server.default.sync;
			// créer les groupes si nécessaire
			Wires_Node.makeGroups;
			// créer un ensemble de noeuds
			nbAudio.do { Wires_InnerNode.basicNew(Wires_Def.randDef('audio', typeWeights)) };
			nbControl.do { Wires_InnerNode.basicNew(Wires_Def.randDef('control', typeWeights)) };
			// démarrer les noeuds
			// while {Wires_Node.availableNodes['audio'] < nbAudio}
			// { Wires_InnerNode.basicNew(Wires_Def.randDef('audio', typeWeights)) };
			// while {Wires_Node.availableNodes['control'] < nbControl}
			// { Wires_InnerNode.basicNew(Wires_Def.randDef('control', typeWeights)) };
			// démarrer les noeuds
			Wires_Node.pool.do {|rate| rate.do(_.start)};
			// créer une sortie
			out = Wires_OutNode.new(volume);

			// boucle de renouvellement
			{
				0.5.wait;
				Wires_Node.pool.choose.choose.replace(typeWeights);
			// 	var cur;
			// 	var size = Wires_Node.allNodes.size;
			// 	(32/size).wait;
			// 	cur = Wires_Node.allNodes.pop;
			// 	Wires_Node.allNodes.insert((size*(1-(2**(4.0.rand.neg)))).round.asInteger, cur);
			// 	cur.renew;
			// 	while {Wires_Node.availableNodes['audio'] < nbAudio}
			// 	{ Wires_InnerNode.new(Wires_Def.randDef('audio', typeWeights))};
			// 	while {Wires_Node.availableNodes['control'] < nbControl}
			// 	{ Wires_InnerNode.new(Wires_Def.randDef('control', typeWeights)) };
			}.loop;
		}.play
	}

	free {
		loop.stop;
		Routine {
			out.release;
			2.wait;
			while {Wires_Node.allNodes.isEmpty.not}
			{Wires_Node.allNodes.first.free};
			Wires_Node.freeGroups;
		}.play;
	}
}
