dot = `
# http://www.graphviz.org/content/cluster

digraph G {

	subgraph cluster_0 {
		label = "Kertész utca"
		node [ style=filled,color=white ]
		style=filled
		color=lightgrey
		a [ label = "kérdés" ]
		b [ label = "jó" ]
		c [ label = "nem" ]
	}

	subgraph cluster_1 {
		label = "Gellért";
		node [style=filled];
		color=blue
		d [ label = "Üdv!" ]
		e [ label = "Vége" ]
	}
	start -> a ;
	a -> b
    a -> c
    b -> a
    e -> a
    c -> d
    d -> e

	start [shape=Mdiamond];
}

`