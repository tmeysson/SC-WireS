// CLASSE D'INTERFACE DE WIRES
Wires {
	var out;

	*new {|volume = 0, nbAudio = 20, nbControl = 50, typeWeights = #[1,1,1,1]|
		^super.new.wiresInit(volume, nbAudio, nbControl, typeWeights);
	}

	wiresInit {|volume, nbAudio, nbControl, typeWeights|
		Routine {
			Server.default.bootSync;
			Wires_Def.setup;
			Server.default.sync;
			// créer les groupes si nécessaire
			Wires_Node.makeGroups;
			// créer un ensemble de noeuds
			while {Wires_Node.availableNodes['audio'] < nbAudio}
			{ Wires_InnerNode.basicNew(Wires_Def.randDef('audio', typeWeights)) };
			while {Wires_Node.availableNodes['control'] < nbControl}
			{ Wires_InnerNode.basicNew(Wires_Def.randDef('control', typeWeights)) };
			// démarrer les noeuds
			Wires_Node.pool.do {|rate| rate.do(_.start)};
			// créer une sortie
			out = Wires_OutNode.new(volume);
		}.play
	}

	free {
		Routine {
			out.release;
			2.wait;
			// Wires_Node.allNodes.do(_.free);
			while {Wires_Node.allNodes.isEmpty.not}
			{Wires_Node.allNodes.first.free};
			Wires_Node.freeGroups;
		}.play;
	}
}
// Wires {
// 	// l'ensemble des instances
// 	classvar <instances;
// 	// verrou d'initialisation
// 	classvar setupLock;
// 	// verrou de renouvellement
// 	classvar <renewLock;
// 	// le groupe parallèle de base
// 	classvar <baseGroup;
//
// 	// la racine du graphe
// 	var <root;
// 	// la Routine de renouvellement
// 	var renew, delay, randTime;
//
// 	*initClass {
// 		instances = List();
// 		setupLock = Semaphore();
// 		renewLock = Semaphore();
// 	}
//
// 	*new {|volume = 0.25, typeWeights, delay = 2, randTime = 0.0,
// 		numNodes = #[40, 10, 2], randNumNodes = 0, numNodeCycle = nil, debug = 0|
// 		^super.new.wiresInit(volume, typeWeights, delay, randTime, numNodes,
// 		randNumNodes, numNodeCycle, debug);
// 	}
//
// 	wiresInit {|volume, typeWeights, dt, rt, numNodes, randNumNodes, numNodeCycle, debug|
// 		var curNumNodes = numNodes;
// 		var cycleDiff, cycleTime, cycleFunc;
// 		if (numNodeCycle.notNil) {
// 			# cycleDiff, cycleTime = numNodeCycle;
// 			cycleDiff = cycleDiff - numNodes;
// 			cycleFunc = {|t| numNodes + (cycleDiff*(t/cycleTime).fold(0,1)).round.asInteger};
// 		};
// 		delay = dt;
// 		randTime = rt;
//
// 		renew = Routine {
// 			var time = 0;
// 			var renewFunc = if (numNodeCycle.notNil)
// 			{
// 				{
// 					var newNumNodes, delta;
// 					protect {
// 						renewLock.wait;
// 						if (instances.includes(this)) {
// 							newNumNodes = cycleFunc.(time);
// 							delta = newNumNodes - curNumNodes;
// 							root.renew((({1.0.rand}!3) * (curNumNodes - [0, 1, 0])).round.max(delta.neg),
// 							delta, nil);
// 							curNumNodes = newNumNodes;
// 							time = time + 1;
// 						};
// 					} {renewLock.signal};
// 				};
// 			} {
// 				{
// 					var newNumNodes, delta;
// 					protect {
// 						renewLock.wait;
// 						if (instances.includes(this)) {
// 							newNumNodes = (numNodes * (({randNumNodes.rand2}!2 + 1)++[1])).round.max([0,2,0]);
// 							delta = (newNumNodes - curNumNodes).asInteger;
// 							root.renew((({1.0.rand}!3) * (curNumNodes - [0, 1, 0])).round.max(delta.neg),
// 							delta, nil);
// 							curNumNodes = newNumNodes;
// 							time = time + 1;
// 						};
// 					} {renewLock.signal};
// 				}
// 			};
// 			protect {
// 				setupLock.wait;
// 				Server.default.bootSync;
// 				Wires_Def.setup;
// 				// créer le groupe de base, si il n'existe pas
// 				if (baseGroup.isNil or: {baseGroup.isRunning.not})
// 				{
// 					baseGroup = ParGroup();
// 					NodeWatcher.register(baseGroup);
// 				};
// 				Server.default.sync;
// 			} {setupLock.signal};
// 			root = Wires_OutNode(volume, typeWeights, numNodes);
// 			instances.add(this);
// 			{
// 				if (debug.bitTest(0)) {
// 					var numSynths = Server.default.numSynths;
// 					var countedNodes = instances.sum {|elt|
// 					elt.root.countNodes(update: true).numNodes};
// 					"[1]Busses: %, Synths: %, Nodes: %"
// 					.format(Server.default.audioBusAllocator.blocks.size +
// 						Server.default.controlBusAllocator.blocks.size,
// 					numSynths, countedNodes.round(0.01)).postln;
// 				};
// 				if (debug.bitTest(1) && ((time % 16) == 0)) {
// 					protect {
// 						var keep, remove;
// 						renewLock.wait;
// 						keep = instances.inject(Set()) {|res, inst| res.union(inst.root.nodeSet)};
// 						remove = Wires_Node.allNodes.removeAll(keep);
// 						if (remove.isEmpty.not) {
// 							"Removing % stray Node(s)".format(remove.size).postln;
// 							// on procede en ordre inverse pour éliminer les noeuds de bas en haut
// 							remove.reverse.do(_.freeStray);
// 						};
// 						"[2]Busses: %, Synths: %, Nodes: %"
// 						.format(Server.default.audioBusAllocator.blocks.size +
// 							Server.default.controlBusAllocator.blocks.size,
// 						Server.default.numSynths, keep.size).postln;
// 					} { renewLock.signal };
// 				};
// 				renewFunc.fork;
// 				(delay * (2 ** rand(randTime))).wait;
// 			}.loop;
// 		}.play;
// 	}
//
// 	*multi {|num = 0, volume = 0.25, typeWeights, numNodes, randNumNodes, numNodeCycle, debug = 0|
// 		Routine {
// 			num.do {
// 				this.new(volume: volume, numNodes: numNodes,
// 					randNumNodes: randNumNodes, numNodeCycle: numNodeCycle,
// 				typeWeights: typeWeights, randTime: 1.0, debug: debug);
// 				debug = 0; 1.wait;
// 			};
// 		}.play;
// 	}
//
// 	stop {
// 		{
// 			protect {
// 				renewLock.wait;
// 				this.stopRenew;
// 				this.freeAll;
// 				// attendre que les Synths soient bien terminés
// 				1.wait;
// 			} {renewLock.signal};
// 		}.fork;
// 	}
//
// 	*stopAll {
// 		var curInstances = instances.copy;
// 		{
// 			protect {
// 				renewLock.wait;
// 				curInstances.do(_.stopRenew);
// 				curInstances.do(_.freeAll);
// 				// attendre que les Synths soient bien terminés
// 				1.wait;
// 			} {renewLock.signal};
// 		}.fork;
// 	}
//
// 	stopRenew {
// 		renew.stop;
// 	}
//
// 	freeAll {
// 		root.release;
// 		instances.remove(this);
// 	}
// }
