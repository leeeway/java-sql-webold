interface RuntimeConfig {
  userName: string;
  version: string;
  serverDomain: string;
}

const globalData: RuntimeConfig = {
  userName: '',
  version: '',
  serverDomain: '',
};

export default globalData;
