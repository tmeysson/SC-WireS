# SC-WireS
*** WireS random audio graph generator (SuperCollider extension)

*** WireS: Générateur aléatoire de graphe audio (extension SuperCollider)

*** Licence: GPL V2

WireS génère un ou plusieurs graphes audio aléatoires, suivant un ensemble de définitions donnés dans la bibliothèque associée.

Le nombre de noeuds (contrôle, audio, réinjection) est configurable à l'initialisation.

Une classe d'interface Wires permet d'effectuer les opérations habituelles (voir le fichier de test extras/Wires.scd)

A FAIRE: documentation SCHelp

*** Principe de fonctionnement:

Les noeuds de sortie sont de type audio.

Chaque noeud emploie un certain nombre d'entrées (audio ou contrôle).

Les quotas de noeuds restants sont répartis entre les sous-noeuds, suivant une définition choisie par aléatoire pondéré parmi les 
définitions compatibles avec le nombre de noeuds disponibles.

Les sous-noeuds peuvent être obtenus par des 'variables', c'est à dire des sous-graphes partagés au niveau de l'application tout entière.

En outre, les noeuds audio peuvent être des noeuds à réinjection (c'est à dire, qui renvoient la sortie d'un graphe choisi aléatoirement).

Le contenu des graphes est renouvellé periodiquement, par une fonction récursive qui remplace un sous-graphe de la taille spécifiée par un nouveau sous-graphe de même taille.

Chaque graphe est diffusé en stéréo selon un Pan qui évolue de façon aléatoire.

*** Installation:

Installer l'extension dans le répertoire des extensions (utilisateur ou global) de SuperCollider.

Pour cela il est possible d'utiliser un lien symbolique vers le clone Git.

Le fichier extras/Wires.scd permet ensuite de configurer et lancer le programme (faire une copie, puis la modifier au besoin).
