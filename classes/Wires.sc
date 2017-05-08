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

	*new {|volume = 0.25, delay = 2, randTime = 0.0|
		^super.new.wiresInit(volume, delay, randTime);
	}

	wiresInit {|volume, dt, rt|
		delay = dt;
		randTime = rt;
		renew = Routine {
			protect {
				setupLock.wait;
				Server.default.bootSync;
				Wires_Def.setup;
				Server.default.sync;
			} {setupLock.signal};
			root = Wires_Node.out(volume);
			{
				(delay * (2 ** rand2(randTime))).wait;
				root.renew((rand(1.0)/* ** 2*/) * (root.numNodes - 1) + 1);
			}.loop;
		}.play;
		instances.add(this);
	}

	*multi {|num = 0, volume = 0.25|
		Routine {
			num.do { this.new(volume: volume, randTime: 1.0); 1.wait; };
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