Wires_InnerNode : Wires_Node {
	// classvar <dummyBus;
	//
	// *makeDummies {
	// 	dummyBus = Dictionary.newFrom([audio: Bus.audio, control: Bus.control]);
	// }

	*basicNew {|def|
		^super.basicNew(def).innerNodeInit;
	}

	*new {|def|
		^this.basicNew(def).start;
	}

	innerNodeInit {/*|noBus = false|
		// créer un Bus de sortie si demandé
		if (noBus) {
			outBus = dummyBus[def.rate];
		} {
			outBus = Bus.alloc(def.rate);
		};*/
		outBus = Bus.alloc(def.rate);
		args = [out: outBus];
		// s'ajouter dans le pool
		pool[def.rate].add(this);
		// nombre de lecteurs
		// readers = 0;
		// lecteurs potentiels et noeuds disponibles
		// potential = 5;
		// availableNodes[def.rate] = availableNodes[def.rate] + 5;
		// availableNodes['control'] = availableNodes['control'] - def.nbSubs[0];
		// availableNodes['audio'] = availableNodes['audio'] - def.nbSubs[1];
	}

	outBus_ {|bus|
		if (outBus.notNil) {outBus.free};
		outBus = bus;
	}

	replace {|typeWeights = #[1,1,1,1]|
		Routine {
			var newNode = this.class.new(Wires_Def.randDef(def.rate, typeWeights));
			var busses;
			lock.wait;
			newNode.lock.wait;
			if (isRunning) {
				busses = {Bus.alloc(def.rate)} ! 2;
				Synth("wires-trans-%".format(def.rate).asSymbol,
					[out: outBus, in1: busses[0], in2: busses[1], transGroup]);
				synth.set('out', busses[0]);
				newNode.synth.set('out', busses[1]);
				1.wait;
				newNode.synth.set('out', outBus);
				newNode.outBus = outBus;
				busses.do(_.free);
				this.free(false);
			};
			newNode.lock.signal;
			lock.signal;
		}.play;
	}

	free {|freeBus = true|
		// supprimer dans pool et dans availableNodes
		// availableNodes[def.rate] = availableNodes[def.rate] - potential;
		if (pool[def.rate].includes(this)) {pool[def.rate].remove(this)};
		^super.free(freeBus);
	}

}
