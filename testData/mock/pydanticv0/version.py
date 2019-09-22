class StrictVersion:
    def __init__(self, version):
        self.version = version


__all__ = ['VERSION']

VERSION = StrictVersion('0.32')
