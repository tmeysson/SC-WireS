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

	*new {|delay = 2, randTime = 0.0, reload = false|
		^super.new.wiresInit(delay, randTime, reload);
	}

	wiresInit {|dt, rt, reload|
		delay = dt;
		randTime = rt;
		renew = Routine {
			protect {
				setupLock.wait;
				Wires_Def.setup(reload);
				if (Server.default.pid.isNil) {Server.default.bootSync};
				// Server.default.sync;
			} {setupLock.signal};
			root = Wires_Node.out;
			{
				(delay * (2 ** rand2(randTime))).wait;
				root.renew((rand(1.0) ** 2) * (root.numNodes - 1) + 1);
			}.loop;
		}.play;
		instances.add(this);
	}

	*multi {|num = 0|
		Routine {
			num.do { this.new; 0.2.wait; };
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