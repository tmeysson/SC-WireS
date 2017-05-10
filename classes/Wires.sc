// CLASSE D'INTERFACE DE WIRES
Wires {
	// l'ensemble des instances
	classvar instances;
	// verrou d'initialisation
	classvar setupLock;

	// la racine du graphe
	var root;
	// la Routine de renouvellement
	var renew, delay, randTime;

	*initClass {
		instances = List();
		setupLock = Semaphore();
	}

	*new {|volume = 0.25, typeWeights, delay = 2, randTime = 0.0, debug = false|
		^super.new.wiresInit(volume, typeWeights, delay, randTime, debug);
	}

	wiresInit {|volume, typeWeights, dt, rt, debug|
		delay = dt;
		randTime = rt;
		renew = Routine {
			protect {
				setupLock.wait;
				Server.default.bootSync;
				Wires_Def.setup;
				Server.default.sync;
			} {setupLock.signal};
			root = Wires_Node.out(volume, typeWeights);
			{
				var numSynths = Server.default.numSynths;
				var numNodes = root.numNodes;
				if (numNodes != numSynths) {root.countNodes(update: true)};
				if (debug) {
					"Busses: %, Synths: %, NumNodes: % -> %"
					.format(Server.default.audioBusAllocator.blocks.size +
						Server.default.controlBusAllocator.blocks.size,
						numSynths, numNodes.round(0.01), root.numNodes).postln;
				};
				root.renew(2 ** rand(log2(root.numNodes) / 0.95));
				(delay * (2 ** rand(randTime))).wait;
			}.loop;
		}.play;
		instances.add(this);
	}

	*multi {|num = 0, volume = 0.25, typeWeights|
		Routine {
			num.do { this.new(volume: volume, typeWeights: typeWeights, randTime: 1.0); 1.wait; };
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
