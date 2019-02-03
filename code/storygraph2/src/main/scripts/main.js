requirejs.config({
    paths: {
        kotlin: 'out/lib/kotlin.js',
        customerBL: 'out/customerBL'
    }
});

requirejs(["customerBL"], function (customerBL) {
    console.log(customerBL)
});