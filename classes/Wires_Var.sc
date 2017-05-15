Wires_Var : Wires_Node {
	// les niveaux de variables
	classvar levels;
	// les groupes des niveaux
	classvar lvlGroups;

	// le nombre de références
	var refs;

	*initClass {
		levels = List();
		lvlGroups = List();
	}

	*getVar {|rate = 'audio', depth = 0, target, varLevel, typeWeights, parent, quota|
		// si le niveau existe
		if (varLevel < levels.size) {
			var level = levels[varLevel][rate];
			// si on doit créer une nouvelle variable
			if((level.size + 1).reciprocal.coin) {
				// créer un noeud
				^this.new(rate, depth, target, varLevel, typeWeights, parent, quota);
			}
			// sinon, retourner une variable existante
			{
				var select = level.choose;
				// incrémenter son compteur de références
				select.incRefs(parent, quota);
				^select;
			}
		}
		// si le niveau n'existe pas, le créer
		{
			lvlGroups.add(ParGroup(lvlGroups[varLevel-1] ? Wires.baseGroup, 'addAfter'));
			levels.add(Dictionary.newFrom([audio: List(), control: List()]));
			// créer une variable et la retourner
			^this.new(rate, depth, target, varLevel, typeWeights, parent, quota);
		}
	}

	*new {|rate = 'audio', depth = 0, target, varLevel, typeWeights, parent, quota|
		^super.new(rate, depth, lvlGroups[varLevel], varLevel+1, typeWeights,
			parent, quota, true).varInit(parent);
	}

	varInit {|parent|
		// initialiser le compteur de références
		refs = 1;
		// ajouter dans les variables
		levels[varLevel-1][outBus.rate].add(this);
		// enregistrer le quota et la date initiales
		quota = Dictionary().put(parent, quota);
		date = Dictionary().put(parent, date);
	}

	quota {|parent|
		// ^quota.values.reduce('max');
		^quota[parent];
	}

	date {|parent|
		^date[parent]
	}

	incRefs {|parent, qt|
		// augmenter le nombre de références
		refs = refs + 1;
		// enregistrer le quota
		quota.put(parent, qt);
		date.put(parent, Date.getDate.rawSeconds);
	}

	decRefs {|parent|
		// décrémenter le nombre de références
		refs = refs - 1;
		// supprimer le quota
		quota.removeAt(parent);
		date.removeAt(parent);
	}

	free {|parent|
		this.decRefs(parent);
		// si il n'y a plus de références
		if (refs == 0)
		{
			// supprimer le noeud
			super.free(parent);
			levels[varLevel-1][outBus.rate].remove(this);
			// si le niveau est vide, le supprimer
			if (levels[varLevel-1].sum(_.size) == 0)
			{
				levels.pop;
				lvlGroups[varLevel-1].free;
				lvlGroups.pop;
			}
		};
		// sinon, ne rien faire
	}

	countNodes {|coeff = 1, update = false|
		// obtenir le bon nombre de noeuds
		// en prenant en compte les références multiples
		^super.countNodes(coeff/refs, update);
	}
}
