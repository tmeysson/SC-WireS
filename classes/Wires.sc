// CLASSE D'INTERFACE DE WIRES
Wires {
	// la racine du graphe
	var root;
	// la Routine de renouvellement
	var renew, delay;

	*new {|delay = 2|
		^super.new.wiresInit(delay);
	}

	wiresInit {|dt|
		Wires_Def.makeDefs(true).makeWeights;
		delay = dt;
		renew = Routine {
			Server.default.bootSync;
			Wires_Def.addDefs;
			Server.default.sync;
			root = Wires_Node.out;
			{
				delay.wait;
				root.renew((rand(1.0) ** 2) * (root.numNodes - 1) + 1);
			}.loop;
		}.play;
	}

	stop {
		renew.stop;
		root.release;
	}
}