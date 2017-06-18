// choisir un type de serveur
// a priori, supernova est plus efficace (une optimisation est mise en place)
Server.program;
Server.supernova;
Server.scsynth;

// démarrer un graphe par l'interface
// silencieux si volume = 0
w = Wires(volume: 0 ,typeWeights: [1, 1, 1, 1], numNodes: [10, 4, 2], randNumNodes: 0.5, debug: true);
// arrêter le graphe
w.stop;

// ensemble de graphes
// paramètres:
// num: nombre de graphes
// volume: volume de chaque graphe (à régler suivant le contexte, valeur basse le plus souvent)
// typeWeights: poids de probabilité de chaque catégorie de noeud
//   - oscillateurs
//   - opérations algébriques
//   - constante numérique
//   - générateur de séquence
// numNodes: nombre de noeuds des graphes (par catégorie)
//   - noeuds de contrôle
//   - noeuds audio
//   - noeuds de réinjection
//  debug: afficher des informations sur le nombre de Synths et de Bus, et le nombre de noeuds estimé
Wires.multi(num: 5, volume: 0/*0.075*/, typeWeights: [1, 1, 1, 1], numNodes: [10, 4, 2], randNumNodes: 0.5, debug: true);
Wires.multi(num: 5, volume: 0.075, typeWeights: [1, 1, 1, 1], numNodes: [10, 4, 2], randNumNodes: 0.5, debug: true);
// arrêter toutes les instances
Wires.stopAll;

// nombre de noeuds cyclique
w = Wires(volume: 0 ,typeWeights: [1, 1, 1, 1], numNodes: [8, 4, 0], numNodeCycle: [[32, 16, 2], 32], debug: true);
// arrêter le graphe
w.stop;

// multiple
Wires.multi(num: 5, volume: 0/*0.075*/, typeWeights: [1, 1, 1, 1], numNodes: [8, 4, 0], numNodeCycle: [[32, 16, 2], 32], debug: true);
// arrêter toutes les instances
Wires.stopAll;
