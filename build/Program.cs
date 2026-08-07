using Build;

using var cancellation = new CancellationTokenSource();
Console.CancelKeyPress += (_, eventArgs) => { eventArgs.Cancel = true; cancellation.Cancel(); };
return await new Composition(args, cancellation.Token).Root.RunAsync();
