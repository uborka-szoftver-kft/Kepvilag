dot = `
# http://www.graphviz.org/content/cluster

digraph G {
    fontsize=10
    labeljust=left
    style=rounded

    node[shape=box]
    edge[arrowhead=vee]


	subgraph cluster_0 {
		label = "Kertész utca"
		node [ style=filled,color=white ]
		style=filled
		color=lightgrey
		a [
            label = <
                <table border="0" cellborder="0" cellspacing="1" style="rounded">
                <tr><td align="center"><b>K</b></td></tr>
                <tr><td align="left">Hogy vagy?</td></tr>
            </table> >
        ]
		b [
            label = <
                <table border="0" cellborder="0" cellspacing="1">
                <tr><td align="center"><b>J</b></td></tr>
                <tr><td align="left">Akkor nagyon örülök Neked! <br/>Hát ezenkívül…</td></tr>
            </table> >
        ]
		c [
            label = <
                <table border="0" cellborder="0" cellspacing="1">
                <tr><td align="center"><b>N</b></td></tr>
                <tr><td align="left">Akkor menj a fürdőbe!</td></tr>
            </table> >
        ]
	}

	subgraph cluster_1 {
		label = "Gellért";
		node [ style=filled,color=white ]
		style=filled
		color=lightgrey
		d [
            label = <
                <table border="0" cellborder="0" cellspacing="1">
                <tr><td align="center"><b>UDV</b></td></tr>
                <tr><td align="left">Élvezze a fürdőt úram!</td></tr>
            </table> >
        ]
		e [
            label = <
                <table border="0" cellborder="0" cellspacing="1">
                <tr><td align="center"><b>VEGE</b></td></tr>
                <tr><td align="left">További szép napot! Jó pihenés otthon!</td></tr>
            </table> >
        ]
	}
	start -> a ;
	a -> b [ headlabel="Jól és te?" ]
    a -> c [ headlabel="Nem túl jól." ]
    b -> a [ arrowhead="empty" ]
    e -> a [ arrowhead="empty" ]
    c -> d [ arrowhead="empty" ]
    d -> e

	start [shape=Mdiamond];
}

`