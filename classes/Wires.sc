// CLASSE D'INTERFACE DE WIRES
Wires {
	// la racine du graphe
	var root;
	// la Routine de renouvellement
	var renew, delay, randTime;

	*new {|delay = 2, randTime = 0.0|
		^super.new.wiresInit(delay, randTime);
	}

	wiresInit {|dt, rt|
		delay = dt;
		randTime = rt;
		renew = Routine {
			Server.default.bootSync;
			Wires_Def.setup;
			Server.default.sync;
			root = Wires_Node.out;
			{
				(delay * (2 ** rand2(randTime))).wait;
				root.renew((rand(1.0) ** 2) * (root.numNodes - 1) + 1);
			}.loop;
		}.play;
	}

	stop {
		renew.stop;
		root.release;
	}
}