declare module 'node-dijkstra' {
    class Graph {
      constructor(graph?: Record<string, Record<string, number>>);
      addNode(name: string, edges: Map<string, number>): this;
      path(start: string, end: string): string[] | null;
    }
    export = Graph;
  }