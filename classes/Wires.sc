// CLASSE D'INTERFACE DE WIRES
Wires {
	// l'ensemble des instances
	classvar <instances;
	// verrou d'initialisation
	classvar setupLock;
	// verrou de renouvellement
	classvar renewLock;
	// le groupe parallèle de base
	classvar <baseGroup;

	// la racine du graphe
	var <root;
	// la Routine de renouvellement
	var renew, delay, randTime;

	*initClass {
		instances = List();
		setupLock = Semaphore();
		renewLock = Semaphore();
	}

	*new {|volume = 0.25, typeWeights, delay = 2, randTime = 0.0,
		numNodes = #[40, 10, 2], randNumNodes = 0, debug = false|
		^super.new.wiresInit(volume, typeWeights, delay, randTime, numNodes, randNumNodes, debug);
	}

	wiresInit {|volume, typeWeights, dt, rt, numNodes, randNumNodes, debug|
		var curNumNodes = numNodes;
		delay = dt;
		randTime = rt;

		renew = Routine {
			protect {
				setupLock.wait;
				Server.default.bootSync;
				Wires_Def.setup;
				// créer le groupe de base, si il n'existe pas
				if (baseGroup.isNil || {baseGroup.isRunning.not})
				{
					baseGroup = ParGroup();
					NodeWatcher.register(baseGroup);
				};
				Server.default.sync;
			} {setupLock.signal};
			root = Wires_OutNode(volume, typeWeights, numNodes);
			instances.add(this);
			{
				root.countNodes(update: true);
				if (debug) {
					var numSynths = Server.default.numSynths;
					var numNodes = instances.sum{|elt| elt.root.numNodes};
					"Busses: %, Synths: %, NumNodes: %"
					.format(Server.default.audioBusAllocator.blocks.size +
						Server.default.controlBusAllocator.blocks.size,
						numSynths, numNodes.round(0.01)).postln;
				};
				{
					var newNumNodes, delta;
					renewLock.wait;
					newNumNodes = (numNodes * (({randNumNodes.rand2}!2 + 1)++[1])).round.max([0,2,0]);
					delta = (newNumNodes - curNumNodes).asInteger;
					root.renew((({1.0.rand}!3) * (curNumNodes - [0, 1, 0])).round.max(delta.neg),
						delta, nil);
					curNumNodes = newNumNodes;
					renewLock.signal;
				}.fork;
				(delay * (2 ** rand(randTime))).wait;
			}.loop;
		}.play;
	}

	*multi {|num = 0, volume = 0.25, typeWeights, numNodes, randNumNodes, debug = false|
		Routine {
			num.do {
				this.new(volume: volume, numNodes: numNodes, randNumNodes: randNumNodes,
					typeWeights: typeWeights, randTime: 1.0, debug: debug);
				debug = false; 1.wait;
			};
		}.play;
	}

	stop {
		{
			renewLock.wait;
			renew.stop;
			root.release;
			instances.remove(this);
			renewLock.signal;
		}.fork;
	}

	*stopAll {
		instances.copy.do(_.stop);
	}
}
