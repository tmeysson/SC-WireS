// CLASSE D'INTERFACE DE WIRES
Wires {
	// l'ensemble des instances
	classvar <instances;
	// verrou d'initialisation
	classvar setupLock;
	// le groupe parallèle de base
	classvar <baseGroup;

	// la racine du graphe
	var <root;
	// la Routine de renouvellement
	var renew, delay, randTime;

	*initClass {
		instances = List();
		setupLock = Semaphore();
	}

	*new {|volume = 0.25, typeWeights, delay = 2, randTime = 0.0, numNodes = #[40, 10, 2], debug = false|
		^super.new.wiresInit(volume, typeWeights, delay, randTime, numNodes, debug);
	}

	wiresInit {|volume, typeWeights, dt, rt, numNodes, debug|
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
				/*
				// ne fonctionne pas avec plusieurs Wires en parallèle
				if (numNodes != numSynths) {root.countNodes(update: true)};
				*/
				root.countNodes(update: true);
				if (debug) {
					var numSynths = Server.default.numSynths;
					var numNodes = instances.sum{|elt| elt.root.numNodes};
					"Busses: %, Synths: %, NumNodes: %"
					.format(Server.default.audioBusAllocator.blocks.size +
						Server.default.controlBusAllocator.blocks.size,
						numSynths, numNodes.round(0.01)).postln;
				};
				{root.renew(2 ** rand(log2(root.numNodes) / 0.95))}.fork;
				(delay * (2 ** rand(randTime))).wait;
			}.loop;
		}.play;
	}

	*multi {|num = 0, volume = 0.25, typeWeights, numNodes, debug = false|
		Routine {
			num.do {
				this.new(volume: volume, numNodes: numNodes, typeWeights: typeWeights,
					randTime: 1.0, debug: debug);
				debug = false; 1.wait;
			};
		}.play;
	}

	stop {
		renew.stop;
		root.release;
		instances.remove(this);
	}

	*stopAll {
		instances.copy.do(_.stop);
	}
}
