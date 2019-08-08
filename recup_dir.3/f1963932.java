
        configurable: true,
        writable: true
      });
    }
    size(chunk) { return 1; }
  }
  defineProperty(global, 'CountQueuingStrategy', {
    value: CountQueuingStrategy,
    enumerable: false,
    configurable: true,
    writable: true
  });
  class BuiltInCountQueuingStrategy {
    constructor(highWaterMark) {
      defineProperty(this, 'highWaterMark', {value: highWaterMark});
    }
    size(chunk) { return 1; }
  }
  binding.createBuiltInCountQueuingStrategy = highWaterMark =>
      new BuiltInCountQueuingStrategy(highWaterMark);
});
8ReadableStream