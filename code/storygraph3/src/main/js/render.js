function render() {
    console.log( "Loaded this:\n" + dot )
    document.getElementById( "dot-source" ).innerHTML = dot
    var svg = Viz( dot, "svg" )
    console.log( "Rendered this:\n" + svg )
    document.getElementById( "dot-rendered" ).innerHTML = svg
}
