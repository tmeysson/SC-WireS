Wires_Node {
	// l'ensemble des noeuds existants
	classvar <allNodes;
	// les listes de noeuds disponibles
	classvar <pool;
	// le nombre total de lectures disponibles
	classvar <availableNodes;
	// les ParGroup
	classvar group, transGroup, outGroup;

	// la définition et les arguments
	var def, args;
	// le Synth
	// var synth;
	// le Synth et son état
	var synth, isRunning;
	// les sous-noeuds
	var subNodes;
	// les noeuds en cours de transition
	// var transNodes;
	// le groupe des sous-noeuds
	// var <subGroup;
	// le Bus de sortie
	var <outBus;
	// la profondeur
	// var depth;
	// le poids des types
	// var typeWeights;
	// le quota de noeuds
	// var <quota;
	// le nombre de noeuds du sous-graphe
	// var <numNodes;
	// la date de création
	var <date;
	// le niveau de variable ciblé
	// var varLevel;
	// lecteurs
	var readers, potential;

	*initClass {
		allNodes = List();
		pool = Dictionary.newFrom([audio: List(), control: List()]);
		availableNodes = Dictionary.newFrom([audio: 0, control: 0]);
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

	// *new {|typeWeights, quota|
	// 	^super.new.nodeInit(typeWeights, quota);
	// }

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
		allNodes.add(this);
		// initialiser l'état
		isRunning = false;
	}

	// nodeInit {
	// 	// date
	// 	date = Date.getDate.rawSeconds;
	// 	// poids des types
	// 	typeWeights = tWghts;
	// 	// le quota de noeuds
	// 	quota = qt.copy;
	// 	// activer le noeud
	// 	isRunning = true;
	// 	// ajouter le noeud à l'ensemble des noeuds
	// 	allNodes.add(this);
	// 	// initialiser les noeuds en transition
	// 	transNodes = List();
	// }

	start {|altGroup|
		// choisir les sous-noeuds
		subNodes = def.synthArgs.collect {|rate, i| if(rate != 'scalar')
			{["p%".format(i).asSymbol, pool[rate].choose.read]}
		}.select(_.notNil);
		// ajouter les sous-noeuds aux arguments
		args = args ++ subNodes.collect {|node| [node[0], node[1].outBus]}.reduce('++');
		// créer le synth
		synth = Synth(def.name, args, altGroup ? group);
		isRunning = true;
	}

	read {
		// ajouter un lecteur
		readers = readers + 1;
		// décrémenter le potentiel
		potential = potential - 1;
		// vérifier si le noeud est toujours lisible
		if (potential == 0) {pool[def.rate].remove(this)};
	}

	drop {
		// supprimer un lecteur
		readers = readers - 1;
		// vérifier si le noeud est toujours utile
		if ((readers == 0) && (potential == 0)) {this.free};
	}

	// makeArgs {|def, target|
	// 	var subQt = (quota[0..1] - def.nbSubs) ++ [quota[2]];
	// 	var audioArgs = def.synthArgs.collectAs({|rate, i| if (rate == 'audio') {i} {nil}}, List)
	// 	.select(_.notNil);
	// 	var fbSubs = List();
	// 	while {subQt[1] < 0}
	// 	{
	// 		var chosen = audioArgs.choose;
	// 		audioArgs.remove(chosen);
	// 		fbSubs.add(chosen);
	// 		subQt[1] = subQt[1] + 1;
	// 		subQt[2] = subQt[2] - 1;
	// 	};
	// 	// calculer les quotas des sous-noeuds
	// 	if (def.synthArgs.notEmpty) {
	// 		subQt = (
	// 			[subQt] ++
	// 			def.synthArgs.collect {|rate, i|
	// 				if (fbSubs.includes(i).not) {
	// 					switch (rate)
	// 					{'scalar'} {[0, 0, 0]}
	// 					{'control'} {[rand(1.0), 0, 0]}
	// 					{'audio'}   {[rand(2.0), rand(1.0), rand(1.0)]};
	// 				} {[0, 0, 0]}
	// 			}
	// 		).flop.collect {
	// 			|it| var a, b; #a ... b = it;
	// 			if (b.any(_!=0)) {
	// 				(b.normalizeSum * a).integrate.round.differentiate.asInteger
	// 			} {b}
	// 		}.flop
	// 	};
	// 	// créer les arguments
	// 	args = def.synthArgs.collect {|rate, i|
	// 		if (fbSubs.includes(i)) {
	// 			[i, 'feedback', subQt[i]]
	// 		} {
	// 			if (rate != 'scalar') {[i, rate, subQt[i]]};
	// 		}
	// 	}.select(_.notNil);
	// 	// créer les sous-noeuds
	// 	if (args.notEmpty) {
	// 		// créer le groupe d'accueil
	// 		group = Group(target);
	// 		// créer un groupe des sous-noeuds
	// 		subGroup = ParGroup(group);
	// 		args = args.collect {|item|
	// 			var i, rate, qt;
	// 			#i, rate, qt = item;
	// 			if (rate != 'feedback') {
	// 				["p%".format(i).asSymbol,
	// 				Wires_InnerNode(rate, depth + 1, subGroup, varLevel, typeWeights, this, qt)]
	// 			} {
	// 				["p%".format(i).asSymbol,
	// 				Wires_FeedBackNode(depth + 1, subGroup, varLevel, typeWeights, this, qt)]
	// 			};
	// 		};
	// 		// enregistrer les sous-noeuds
	// 		subNodes = args;
	// 		// obtenir les Bus de sortie
	// 		args = args.collect {|e| [e[0], e[1].outBus] };
	// 		// aplatir la liste
	// 		args = args.reduce('++');
	// 	} {
	// 		// il n'y a pas de sous-noeuds
	// 		subNodes = [];
	// 	};
	// 	numNodes = 1 + subNodes.sum {|e| e[1].numNodes};
	// }
	//
	// makeSynth {|target|
	// 	synth = Synth(def.name, args, target, 'addToTail');
	// }
	//
	// renew {|minQt, delta, parent|
	// 	var res;
	// 	var panel;
	// 	// appliquer le différentiel
	// 	var newDelta = this.updateQuota(delta, parent);
	// 	var newMinQt = minQt.max(newDelta.neg);
	// 	// tableau des indices des sous-noeuds qui honorent le minimum
	// 	panel = subNodes.collect {|n, i| [i, (n[1].quota(this) - newMinQt).every(_>=0)] }.select(_[1]);
	// 	if (panel.isEmpty) {
	// 		// renouveller ce noeud
	// 		^this.replace(parent);
	// 	} {
	// 		var index, select, node;
	// 		var newNode;
	// 		// choisir un des sous-noeuds
	// 		index = panel.choose[0];
	// 		select = subNodes[index];
	// 		node = select[1];
	// 		newNode = node.renew(newMinQt, newDelta, this);
	// 		if (newNode != node) {
	// 			// effectuer la transition
	// 			var bus, rate;
	// 			transNodes.add(node);
	// 			subNodes[index][1] = newNode;
	// 			rate = node.outBus.rate;
	// 			bus = Bus.alloc(rate);
	// 			Synth("wires-trans-%".format(rate).asSymbol,
	// 				[out: bus, in1: node.outBus, in2: newNode.outBus],
	// 			synth, 'addBefore').onFree {bus.free};
	// 			synth.set(select[0], bus);
	// 			{
	// 				// attendre la fin de la transition
	// 				1.wait;
	// 				// terminer la transition
	// 				if (isRunning) {synth.set(select[0], newNode.outBus)};
	// 				protect {
	// 					Wires.renewLock.wait;
	// 					node.free(this);
	// 					transNodes.remove(node);
	// 				} { Wires.renewLock.signal };
	// 			}.fork;
	// 		};
	// 		// retourner le noeud courant
	// 		^this;
	// 	};
	// }
	//
	// updateQuota {|delta|
	// 	quota = quota + delta;
	// 	^delta;
	// }

	free {
		// supprimer dans allNodes
		allNodes.remove(this);
		// libérer le Synth et le Bus
		if (isRunning) {synth.free; isRunning = false};
		outBus.free;
		// cesser de lire les subNodes et les transNodes
		subNodes.do {|node| node[1].drop};
		// transNodes.do(_.drop);
		// libérer l'objet
		^super.free;
	}

	renew {
		var index = subNodes.size.rand;
		// choisir un des sous-noeuds
		var node = subNodes[index];
		var rate = node[1].outBus.rate;
		// créer un Bus
		var bus = Bus.alloc(rate);
		// choisir un noeud de remplacement
		var newNode = pool[rate].choose;
		// lire le nouveau noeud
		newNode.read;
		// effectuer la permutation
		// transNodes.add(node[1]);
		subNodes[index] = [node[0], newNode];
		Routine {
			// démarrer la transition
			Synth("wires-trans-%".format(rate).asSymbol,
				[out: bus, in1: node.outBus, in2: newNode.outBus],
				transGroup);
			synth.set([node[0], bus]);
			// attendre la fin
			1.wait;
			// terminer la transition
			synth.set([node[0], newNode.outBus]);
			// libérer le Bus
			bus.free;
			// cesser de lire l'ancien noeud
			node[1].drop;
			// le supprimer des transNodes
			// transNodes.remove(node[1]);
		}.play;
	}

	// free {|parent|
	// 	subNodes.do {|node| node[1].free(this) };
	// 	if (isRunning) {
	// 		synth.free; isRunning = false;
	// 		if (subGroup.notNil) {subGroup.free};
	// 		if (group.notNil) {group.free};
	// 		if (outBus.notNil) {outBus.free};
	// 	};
	// 	allNodes.remove(this);
	// }

	// freeStray {
	// 	if (isRunning) {
	// 		synth.free; isRunning = false;
	// 		if (subGroup.notNil) {subGroup.free};
	// 		if (group.notNil) {group.free};
	// 		if (outBus.notNil) {outBus.free};
	// 	};
	// 	allNodes.remove(this);
	// }

	// release {
	// 	synth.release;
	// 	while {isRunning} {0.1.wait};
	// }
	//
	// countNodes {|coeff = 1, update = false|
	// 	var count = subNodes.sum {|n| n[1].countNodes(coeff, update)} + coeff;
	// 	if (update) {numNodes = count};
	// 	^count;
	// }
	//
	// nodeSet {
	// 	^(subNodes.collect(_[1]) ++ transNodes).inject(Set[this])
	// 	{|res, sub| res.union(sub.nodeSet)};
	// }
}
