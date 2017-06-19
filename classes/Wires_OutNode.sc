Wires_OutNode : Wires_Node {
	var <inBus;

	*new {|volume = 0.25, typeWeights, quota|
		^super.new(typeWeights, quota).outNodeInit(volume);
	}

	outNodeInit {|volume|
		// profondeur
		depth = -1;
		// niveau de variable
		varLevel = 0;
		// définition
		def = Wires_Def.outDef;
		// créer les arguments
		this.makeArgs(def, Wires.baseGroup);
		// enregistrer le Bus d'entrée
		inBus = args[1];
		// ajouter le volume
		args = [vol: volume] ++ args;
		// créer le Synth
		this.makeSynth(group);
		// libérer le sous-graphe à la fin
		synth.onFree {isRunning = false; this.free};
	}

	renew {|minQt, delta, parent = nil|
		var select, node;
		var newNode;
		// appliquer le différentiel
		// NOTE: newDelta == delta et newMinQt == minQt, dans le cas d'un OutNode
		var newDelta = this.updateQuota(delta, parent);
		var newMinQt = minQt.max(newDelta.neg);
		// choisir le sous-noeud
		select = subNodes[0];
		node = select[1];
		newNode = node.renew(newMinQt, newDelta, this);
		if (newNode != node) {
			// effectuer la transition
			var bus, rate;
			subNodes[0][1] = newNode;
			{
				rate = node.outBus.rate;
				bus = Bus.alloc(rate);
				Synth("wires-trans-%".format(rate).asSymbol,
					[out: bus, in1: node.outBus, in2: newNode.outBus],
					synth, 'addBefore').onFree {bus.free};
				synth.set(select[0], bus);
				// attendre la fin de la transition
				1.wait;
				// terminer la transition
				synth.set(select[0], newNode.outBus);
				node.free;
			}.fork;
		};
		// retourner le noeud courant
		^this;
	}
}
