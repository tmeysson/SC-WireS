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

	*getVar {|rate = 'audio', depth = 0, target, varLevel, typeWeights|
		// si le niveau existe
		if (varLevel < levels.size) {
			var level = levels[varLevel][rate];
			// si on doit créer une nouvelle variable
			if((level.size + 1).reciprocal.coin) {
				// créer un noeud
				^this.new(rate, depth, target, varLevel, typeWeights);
			}
			// sinon, retourner une variable existante
			{
				var select = level.choose;
				// incrémenter son compteur de références
				select.incRefs;
				^select;
			}
		}
		// si le niveau n'existe pas, le créer
		{
			lvlGroups.add(ParGroup(lvlGroups[varLevel-1] ? baseGroup, 'addAfter'));
			levels.add(Dictionary.newFrom([audio: List(), control: List()]));
			// créer une variable et la retourner
			^this.new(rate, depth, target, varLevel, typeWeights);
		}
	}

	*new {|rate = 'audio', depth = 0, target, varLevel, typeWeights|
		^super.new(rate, depth, lvlGroups[varLevel], varLevel+1, typeWeights, true).varInit;
	}

	varInit {
		// initialiser le compteur de références
		refs = 1;
		// ajouter dans les variables
		levels[varLevel-1][outBus.rate].add(this);
	}

	incRefs { refs = refs + 1 }

	decRefs { refs = refs - 1 }

	free {
		this.decRefs;
		// si il n'y a plus de références
		if (refs == 0)
		{
			// supprimer le noeud
			super.free;
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
}
