export const Image = {
  getSize: (
    _uri: string,
    success: (w: number, h: number) => void,
    _failure?: () => void,
  ) => success(400, 400),
};

export const Platform = { OS: 'ios', select: (obj: Record<string, unknown>) => obj.default };

export const NativeModules = {};
