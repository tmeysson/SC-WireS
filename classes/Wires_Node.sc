Wires_Node {
	// l'ensemble des noeuds existants
	classvar <allNodes;
	// les listes de noeuds disponibles
	classvar <pool;
	// le nombre total de lectures disponibles
	// classvar <availableNodes;
	// les ParGroup
	classvar group, transGroup, outGroup;

	// la définition et les arguments
	var def, args;
	// le Synth et son état
	var <synth, isRunning;
	// les sous-noeuds
	var subNodes;
	// les noeuds en cours de transition
	// var transNodes;
	// le Bus de sortie
	var <outBus;
	// le poids des types
	// var typeWeights;
	// la date de création
	var <date;
	// lecteurs
	// var readers, potential;
	// verrou
	var <lock;

	*initClass {
		allNodes = List();
		pool = Dictionary.newFrom([audio: List(), control: List()]);
		// availableNodes = Dictionary.newFrom([audio: 0, control: 0]);
	}

	*makeGroups {
		if (group.isNil) {group = ParGroup()};
		if (transGroup.isNil) {transGroup = ParGroup(group, 'addAfter')};
		if (outGroup.isNil) {outGroup = ParGroup(transGroup, 'addAfter')};
	}

	*freeGroups {
		if (group.notNil) {group.free; group = nil};
		if (transGroup.notNil) {transGroup.free; transGroup = nil};
		if (outGroup.notNil) {outGroup.free; outGroup = nil};
	}

	*basicNew {|def|
		^super.new.nodeInit(def);
	}

	*new {|def|
		^this.basicNew(def).start;
	}

	nodeInit {|nodeDef|
		// date
		date = Date.getDate.rawSeconds;
		// initialiser les noeuds en transition
		// transNodes = List();
		// définition
		def = nodeDef;
		// s'ajouter dans allNodes
		allNodes.addFirst(this);
		// initialiser l'état
		isRunning = false;
		// verrou
		lock = Semaphore();
	}

	start {|altGroup|
		// choisir les sous-noeuds
		subNodes = def.synthArgs.collect {|rate, i| if(rate != 'scalar')
			{["p%".format(i).asSymbol, pool[rate].choose/*.read*/]}
		}.select(_.notNil);
		// ajouter les sous-noeuds aux arguments
		args = args ++ subNodes.collect {|node| [node[0], node[1].outBus]}.reduce('++');
		// créer le synth
		synth = Synth(def.name, args, altGroup ? group);
		isRunning = true;
	}

	// read {
	// 	// ajouter un lecteur
	// 	readers = readers + 1;
	// 	// décrémenter le potentiel
	// 	potential = potential - 1;
	// 	availableNodes[def.rate] = availableNodes[def.rate] - 1;
	// 	// vérifier si le noeud est toujours lisible
	// 	if (potential == 0) {pool[def.rate].remove(this)};
	// }

	// drop {
	// 	// supprimer un lecteur
	// 	readers = readers - 1;
	// 	// vérifier si le noeud est toujours utile
	// 	if ((readers == 0) && (potential == 0)) {this.free};
	// }

	free {|freeBus = true|
		// supprimer dans allNodes
		allNodes.remove(this);
		// libérer le Synth et le Bus
		if (isRunning) {isRunning = false; synth.free};
		if (freeBus && outBus.notNil and: {outBus.index.notNil}) {outBus.free};
		// cesser de lire les subNodes et les transNodes
		// subNodes.do {|node| node[1].drop};
		// transNodes.do(_.drop);
		// libérer l'objet
		^super.free;
	}

	// renew {
	// 	if (subNodes.size == 0) {^false} {
	// 		var index = subNodes.size.rand;
	// 		// choisir un des sous-noeuds
	// 		var node = subNodes[index];
	// 		var rate = node[1].outBus.rate;
	// 		// créer un Bus
	// 		var bus = Bus.alloc(rate);
	// 		// choisir un noeud de remplacement
	// 		var newNode = pool[rate].choose;
	// 		// lire le nouveau noeud
	// 		newNode.read;
	// 		// effectuer la permutation
	// 		// transNodes.add(node[1]);
	// 		subNodes[index] = [node[0], newNode];
	// 		Routine {
	// 			// démarrer la transition
	// 			Synth("wires-trans-%".format(rate).asSymbol,
	// 				[out: bus, in1: node[1].outBus, in2: newNode.outBus],
	// 			transGroup);
	// 			synth.set(node[0], bus);
	// 			// attendre la fin
	// 			1.wait;
	// 			// terminer la transition
	// 			synth.set(node[0], newNode.outBus);
	// 			// libérer le Bus
	// 			bus.free;
	// 			// cesser de lire l'ancien noeud
	// 			node[1].drop;
	// 			// le supprimer des transNodes
	// 			// transNodes.remove(node[1]);
	// 		}.play;
	// 		^true;
	// 	}
	// }
}
