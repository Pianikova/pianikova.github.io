window.pianikovaDocuments = {
    copy: async function (selector) {
        const element = document.querySelector(selector);
        if (!element) return;
        await navigator.clipboard.writeText(element.innerText.trim());
    },
    print: function (id) {
        const element = document.getElementById(id);
        if (!element) return;

        const printHost = document.createElement("div");
        printHost.className = "print-host";
        printHost.setAttribute("aria-hidden", "true");
        printHost.appendChild(element.cloneNode(true));
        document.body.appendChild(printHost);
        document.body.classList.add("printing-document");

        let cleanedUp = false;
        const cleanup = function () {
            if (cleanedUp) return;
            cleanedUp = true;
            document.body.classList.remove("printing-document");
            printHost.remove();
            window.removeEventListener("afterprint", cleanup);
        };

        window.addEventListener("afterprint", cleanup);
        window.print();
        window.setTimeout(cleanup, 60000);
    }
};
