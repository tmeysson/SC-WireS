/*
volume:                       0.25
nb sorties:                   3
nb noeuds audio:              10
nb noeuds controle:           30
nb noeuds remplaces par tour: 8
poids:
oscillateurs:                 3
arithmetiques:                1
constantes:                   1
sequences:                    2
*/

Server.supernova;
Server.scsynth;

w = Wires(0.025, 3, 10, 30, 8, [3,1,1,2]);

w.free;

(
"N:% A:% K:%".format(
	Wires_Node.allNodes.size,
	Server.default.audioBusAllocator.blocks.size,
	Server.default.controlBusAllocator.blocks.size)
)
