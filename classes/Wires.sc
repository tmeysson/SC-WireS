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

	*new {|volume = 0.25, typeWeights, delay = 2, randTime = 0.0|
		^super.new.wiresInit(volume, typeWeights, delay, randTime);
	}

	wiresInit {|volume, typeWeights, dt, rt|
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
				(delay * (2 ** rand(randTime))).wait;
				root.renew(2 ** rand(log2(root.numNodes) / 0.95));
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