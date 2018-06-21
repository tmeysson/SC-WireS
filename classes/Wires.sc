// CLASSE D'INTERFACE DE WIRES
Wires {
	var outs;
	var loop;

	*new {|volume = 0, outMode = 'stereo', nbOuts = 1, nbAudio = 5, nbControl = 10,
		timeReplace = 2, nbReplace = 4, typeWeights = #[1,1,1,1]|
		^super.new.wiresInit(volume, outMode, nbOuts, nbAudio, nbControl,
			timeReplace, nbReplace, typeWeights);
	}

	wiresInit {|volume, outMode, nbOuts, nbAudio, nbControl, timeReplace, nbReplace, typeWeights|
		var mode = if (outMode == 'chan') {{|n|n}} {outMode};
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
			// outs = {Wires_OutNode.new(volume)} ! nbOuts;
			outs = Wires_Node.pool['audio'].scramble[..nbOuts-1].collect
			{|node, i| Wires_OutNode.new(volume, node, mode.(i))};

			// boucle de renouvellement
			{
				timeReplace.wait;
				Wires_Node.pool.values.reduce('++').select(_.isRunning)
				.scramble[..nbReplace-1].do(_.replace(typeWeights));
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
			outs.do(_.release);
			2.wait;
			while {Wires_Node.allNodes.isEmpty.not}
			{Wires_Node.allNodes.first.free};
			Wires_Node.freeGroups;
		}.play;
	}
}
