window.pianikovaDocuments = {
    copy: async function (selector) {
        const element = document.querySelector(selector);
        if (!element) return;
        await navigator.clipboard.writeText(element.innerText.trim());
    },
    print: function (id) {
        const element = document.getElementById(id);
        if (!element) return;
        element.classList.add("print-target");
        document.body.classList.add("printing-document");
        const cleanup = function () {
            document.body.classList.remove("printing-document");
            element.classList.remove("print-target");
            window.removeEventListener("afterprint", cleanup);
        };
        window.addEventListener("afterprint", cleanup);
        window.print();
        window.setTimeout(cleanup, 1000);
    }
};
